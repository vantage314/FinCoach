"""
FinCoach F10 资料全量填充脚本
一次性运行：抓取公司资料(Profile)、公司公告(Notice)、财务摘要(Report)、新闻(News)
"""
import sys
import time
import datetime
from sqlalchemy import create_engine, text

# ===================== 配置 =====================
DB_URL = "postgresql+psycopg2://postgres:030314@localhost:5432/fincoach"

try:
    engine = create_engine(DB_URL, pool_recycle=3600, pool_pre_ping=True)
    print("[INFO] 数据库连接成功")
except Exception as e:
    print(f"[ERROR] 数据库连接失败: {e}")
    sys.exit(1)

# ===================== 抓取逻辑 =====================
def fetch_all():
    try:
        import akshare as ak
    except ImportError:
        print("[ERROR] 请先安装 akshare: pip install akshare")
        sys.exit(1)

    # 1. 获取所有股票代码
    with engine.connect() as conn:
        result = conn.execute(text("SELECT code, name FROM market_security WHERE type = 'STOCK'"))
        stocks = [(str(row[0]), str(row[1])) for row in result.fetchall()]

    if not stocks:
        print("[WARN] 数据库中无股票记录")
        return

    print(f"[INFO] 共找到 {len(stocks)} 只股票，开始全量抓取 F10...\n")

    # 2. 遍历抓取 F10
    for i, (code, name) in enumerate(stocks):
        try:
            print(f"[{i+1}/{len(stocks)}] 处理 {name}({code})...")
            
            # --- A. 公司主页信息 (Company Profile) ---
            try:
                # 接口: stock_individual_info_em
                df_info = ak.stock_individual_info_em(symbol=code)
                if df_info is not None and not df_info.empty:
                    info_dict = dict(zip(df_info['item'].tolist(), df_info['value'].tolist()))
                    
                    company_name = info_dict.get('股票简称', name)
                    chairman = info_dict.get('董事长', '')
                    listing_date = info_dict.get('上市时间') 
                    # 格式化日期 YYYYMMDD -> YYYY-MM-DD
                    if listing_date and len(str(listing_date)) == 8:
                        listing_date = f"{str(listing_date)[:4]}-{str(listing_date)[4:6]}-{str(listing_date)[6:]}"

                    website = info_dict.get('公司网址', '')
                    scope = info_dict.get('行业', '') # 这里简略用行业代替经营范围
                    
                    # UPSERT company_profile
                    with engine.begin() as conn:
                        exists = conn.execute(
                            text("SELECT COUNT(*) FROM company_profile WHERE stock_code = :code"),
                            {'code': code}
                        ).scalar()
                        
                        if exists == 0:
                            conn.execute(text("""
                                INSERT INTO company_profile (stock_code, company_name, listing_date, chairman, website, business_scope)
                                VALUES (:code, :name, :ld, :cm, :web, :scope)
                            """), {'code': code, 'name': company_name, 'ld': listing_date, 'cm': chairman, 'web': website, 'scope': scope})
                        else:
                            conn.execute(text("""
                                UPDATE company_profile 
                                SET company_name=:name, listing_date=:ld, chairman=:cm, website=:web, business_scope=:scope
                                WHERE stock_code=:code
                            """), {'code': code, 'name': company_name, 'ld': listing_date, 'cm': chairman, 'web': website, 'scope': scope})
                    print("  -> Profile [OK]")
                else:
                    print("  -> Profile 无数据")
            except Exception as e:
                print(f"  -> Profile [ERROR]: {e}")

            # --- B. 公司公告 (Company Notice) ---
            try:
                # 接口: stock_notice_report (最近的公告)
                df_notice = ak.stock_notice_report(symbol=code)
                if df_notice is not None and not df_notice.empty:
                    # 取最近 5 条
                    recent_notices = df_notice.head(5)
                    with engine.begin() as conn:
                        for _, row in recent_notices.iterrows():
                            # 字段: 公告标题, 公告类型, 公告日期, 公告代码(需拼URL)
                            title = row.get('公告标题', '')
                            n_type = row.get('公告类型', '公告')
                            date_str = str(row.get('公告日期', ''))
                            if len(date_str) > 10: date_str = date_str[:10]
                            
                            # 简单的去重检查
                            c = conn.execute(text("SELECT COUNT(*) FROM company_notice WHERE stock_code=:code AND title=:title"),
                                           {'code': code, 'title': title}).scalar()
                            
                            if c == 0:
                                conn.execute(text("""
                                    INSERT INTO company_notice (stock_code, title, type, publish_date)
                                    VALUES (:code, :title, :type, :date)
                                """), {'code': code, 'title': title, 'type': n_type, 'date': date_str})
                    print(f"  -> Notice [OK] ({len(recent_notices)}条)")
                else:
                     print("  -> Notice 无数据")
            except Exception as e:
                print(f"  -> Notice [ERROR]: {e}")

            time.sleep(1) # 稍微慢点防止封IP

        except Exception as e:
            print(f"[ERROR] 处理 {code} 失败: {e}")

    print("\n[INFO] F10 资料抓取完成!")

# ===================== 新闻抓取 =====================
def fetch_news():
    print("\n[INFO] 开始补充新闻数据...")
    try:
        import akshare as ak
        # 接口: stock_telegraph_cls (财联社电报)
        df = ak.stock_telegraph_cls()
        if df is not None and not df.empty:
            count = 0
            with engine.begin() as conn:
                for _, row in df.iterrows():
                    title = row.get('title', '')
                    if not title: title = row.get('content', '')[:30]
                    content = row.get('content', '')
                    ptime = row.get('publish_time', '')
                    if not ptime: ptime = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')

                    # 检查重复
                    exists = conn.execute(text("SELECT COUNT(*) FROM financial_news WHERE content = :c"), {'c': content}).scalar()
                    if exists == 0:
                        conn.execute(text("""
                            INSERT INTO financial_news (title, content, source, publish_time)
                            VALUES (:t, :c, '财联社', :p)
                        """), {'t': title, 'c': content, 'p': ptime})
                        count += 1
            print(f"[INFO] 新增新闻: {count} 条")
        else:
            print("[WARN] 未获取到新闻数据")

    except Exception as e:
        print(f"[ERROR] 新闻抓取失败: {e}")

# ===================== 入口 =====================
if __name__ == "__main__":
    fetch_all()
    fetch_news()
