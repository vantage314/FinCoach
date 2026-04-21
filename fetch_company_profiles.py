"""
FinCoach F10 公司资料抓取脚本 (一次性运行)
使用 akshare 东财接口抓取: 总市值、行业、上市时间、董事长等

用法: python fetch_company_profiles.py
"""
import sys
import time
from sqlalchemy import create_engine, text

# ===================== 配置 =====================
DB_URL = "postgresql+psycopg2://postgres:030314@localhost:5432/fincoach"

try:
    engine = create_engine(DB_URL, pool_recycle=3600, pool_pre_ping=True)
    print("✅ 数据库连接成功")
except Exception as e:
    print(f"❌ 数据库连接失败: {e}")
    sys.exit(1)

# ===================== 主逻辑 =====================
def fetch_and_update():
    try:
        import akshare as ak
    except ImportError:
        print("❌ 请先安装 akshare: pip install akshare")
        sys.exit(1)

    # 1. 从数据库获取所有 stock 类型的代码
    with engine.connect() as conn:
        result = conn.execute(text("SELECT code, name FROM market_security WHERE type = 'STOCK'"))
        stocks = [(str(row[0]), str(row[1])) for row in result.fetchall()]

    if not stocks:
        print("⚠️ 数据库中无股票记录")
        return

    print(f"📋 共找到 {len(stocks)} 只股票，开始抓取 F10 资料...\n")

    success_count = 0
    for code, name in stocks:
        try:
            print(f"  抓取 {name}({code})...", end="")
            
            # 使用东财个股信息接口
            df = ak.stock_individual_info_em(symbol=code)
            
            if df is None or df.empty:
                print(" -> 无数据")
                continue

            # df 格式: item/value 两列
            info_dict = dict(zip(df['item'].tolist(), df['value'].tolist()))
            
            # 提取关键字段
            company_name = info_dict.get('股票简称', name)
            total_market_cap = info_dict.get('总市值', None)
            industry = info_dict.get('行业', '')
            listing_date = info_dict.get('上市时间', None)
            
            # 更新 market_security 表的市值
            with engine.begin() as conn:
                if total_market_cap:
                    try:
                        cap_val = float(total_market_cap)
                        conn.execute(text("""
                            UPDATE market_security SET market_cap = :cap WHERE code = :code
                        """), {'cap': cap_val, 'code': code})
                    except:
                        pass

            # 更新 company_profile 表 (UPSERT)
            with engine.begin() as conn:
                exists = conn.execute(
                    text("SELECT COUNT(*) FROM company_profile WHERE stock_code = :code"),
                    {'code': code}
                ).scalar()

                if exists == 0:
                    conn.execute(text("""
                        INSERT INTO company_profile (stock_code, company_name, listing_date, business_scope)
                        VALUES (:code, :name, :listing, :scope)
                    """), {
                        'code': code,
                        'name': company_name,
                        'listing': listing_date,
                        'scope': industry
                    })
                else:
                    # 更新已有记录
                    conn.execute(text("""
                        UPDATE company_profile 
                        SET company_name = :name, 
                            business_scope = COALESCE(business_scope, :scope)
                        WHERE stock_code = :code
                    """), {
                        'name': company_name,
                        'scope': industry,
                        'code': code
                    })

            success_count += 1
            print(f" -> ✅ {company_name} | 市值: {total_market_cap} | 行业: {industry}")
            
            # 防止请求过快被封
            time.sleep(1)
            
        except Exception as e:
            print(f" -> ❌ 失败: {e}")
            time.sleep(2)
            continue

    print(f"\n🎉 F10 抓取完成! 成功: {success_count}/{len(stocks)}")

# ===================== 入口 =====================
if __name__ == "__main__":
    print("\n🔍 FinCoach F10 公司资料抓取工具")
    print("=" * 40)
    fetch_and_update()
