import akshare as ak
import pandas as pd
from sqlalchemy import create_engine, text
import time
import schedule
import datetime
import random
import sys

# ================= 配置区域 =================
DB_USER = 'postgres'
DB_PASS = '030314'
DB_HOST = 'localhost'
DB_PORT = '5432'
DB_NAME = 'fincoach'

DB_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# ================= 数据库连接 =================
try:
    engine = create_engine(DB_URL, pool_recycle=3600, pool_pre_ping=True)
    print(f"✅ 数据库连接配置完成: {DB_NAME}")
except Exception as e:
    print(f"❌ 数据库引擎创建失败: {e}")
    sys.exit(1)

# ================= 核心工具函数 =================

def get_existing_codes():
    """获取数据库中关注的股票代码"""
    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT code FROM market_security WHERE type = 'STOCK' OR type = 'stock'"))
            codes = [row[0] for row in result.fetchall()]
        return codes
    except Exception as e:
        print(f"⚠️ 获取股票代码失败: {e}")
        return []

def safe_float(value):
    """安全转换浮点数"""
    try:
        return float(value)
    except:
        return 0.0

# ================= 策略1：获取行情 (多源灾备) =================

def fetch_quotes_em():
    """来源A: 东方财富 (最全，但易封IP)"""
    try:
        df = ak.stock_zh_a_spot_em()
        # 映射列名: 代码->code, 最新价->price, 涨跌幅->change
        df = df.rename(columns={'代码': 'code', '最新价': 'price', '涨跌幅': 'change'})
        return df
    except:
        return None

def fetch_quotes_tx():
    """来源B: 腾讯接口 (备用，稳定)"""
    # 腾讯通常没有一次性拉取全市场的简单接口，这里暂用 实时行情接口 的变体
    # 为了简化，我们用 ak.stock_zh_a_spot() 新浪源作为 B 计划，如果还不行就报错
    try:
        df = ak.stock_zh_a_spot()
        # 新浪返回列名通常是 symbol, code, name, trade, changepercent
        df = df.rename(columns={'trade': 'price', 'changepercent': 'change'})
        return df
    except:
        return None

def update_market_quotes():
    """主任务: 更新实时行情"""
    timestamp = datetime.datetime.now().strftime('%H:%M:%S')
    print(f"[{timestamp}] 正在更新行情...", end="")
    
    target_codes = get_existing_codes()
    if not target_codes:
        print(" -> 数据库无股票，跳过")
        return

    # --- 尝试获取数据 ---
    df_all = fetch_quotes_em() # 优先用东财
    source = "东方财富"
    
    if df_all is None or df_all.empty:
        # 如果东财挂了，休息2秒尝试新浪
        time.sleep(2)
        df_all = fetch_quotes_tx()
        source = "新浪/腾讯"

    if df_all is None or df_all.empty:
        print(" -> ❌ 所有接口均响应失败 (IP可能被封，休息20秒)")
        time.sleep(20) 
        return

    # --- 数据入库 ---
    try:
        # 筛选关注的股票
        df_target = df_all[df_all['code'].isin(target_codes)].copy()
        
        if df_target.empty:
            print(f" -> {source} 正常，但无匹配股票")
            return

        updated_count = 0
        with engine.begin() as conn:
            for _, row in df_target.iterrows():
                code = row['code']
                price = safe_float(row.get('price', 0))
                change = safe_float(row.get('change', 0))
                
                # 执行更新
                sql = text("""
                    UPDATE market_security 
                    SET current_price = :price, 
                        change_percent = :change 
                    WHERE code = :code
                """)
                conn.execute(sql, {'price': price, 'change': change, 'code': code})
                updated_count += 1
                
        print(f" -> ✅ ({source}) 更新 {updated_count} 只股票")

    except Exception as e:
        print(f" -> ❌ 入库失败: {e}")

# ================= 策略2：获取新闻 (多源灾备) =================

def fetch_news_cls():
    """来源A: 财联社"""
    try:
        return ak.stock_telegraph_cls(symbol="A股24小时电报")
    except:
        return None

def fetch_news_em():
    """来源B: 东方财富"""
    try:
        # 尝试获取个股新闻列表作为替代
        return ak.stock_news_em(symbol="300059") 
    except:
        return None

def update_financial_news():
    """主任务: 获取新闻"""
    timestamp = datetime.datetime.now().strftime('%H:%M:%S')
    print(f"[{timestamp}] 正在获取新闻...", end="")
    
    df_news = fetch_news_cls()
    source = "财联社"
    
    if df_news is None or df_news.empty:
        df_news = fetch_news_em()
        source = "东方财富"
        
    if df_news is None or df_news.empty:
        print(" -> ⚠️ 所有新闻接口暂不可用")
        return

    try:
        count = 0
        with engine.begin() as conn:
            for _, row in df_news.head(5).iterrows():
                # 智能识别列名
                title = row.get('标题', row.get('title', ''))
                content = row.get('内容', row.get('content', ''))
                pub_time = row.get('发布时间', row.get('public_time', datetime.datetime.now()))
                
                if not content: continue
                if not title: title = content[:30] + "..."

                # 查重
                check_sql = text("SELECT COUNT(*) FROM financial_news WHERE content = :content")
                exists = conn.execute(check_sql, {'content': str(content)}).scalar()
                
                if exists == 0:
                    insert_sql = text("""
                        INSERT INTO financial_news (title, summary, content, source, publish_time)
                        VALUES (:title, :summary, :content, :source, :pub_time)
                    """)
                    conn.execute(insert_sql, {
                        'title': str(title)[:200],
                        'summary': str(content)[:100],
                        'content': str(content),
                        'source': source,
                        'pub_time': pub_time
                    })
                    count += 1
        
        print(f" -> ✅ ({source}) 新增 {count} 条")
        
    except Exception as e:
        print(f" -> ⚠️ 处理失败: {e}")

# ================= 主程序 =================

def main():
    print("\n🚀 全能兼容版同步服务 v3.0 已启动")
    print("-----------------------------------")
    print("功能策略: 双源热备 (东财/新浪) + 智能字段映射")
    print("更新频率: 行情 15秒/次 | 新闻 2分钟/次")
    print("-----------------------------------")

    # 立即执行
    update_market_quotes()
    update_financial_news()

    # 降低频率，防止封号
    schedule.every(15).seconds.do(update_market_quotes)
    schedule.every(2).minutes.do(update_financial_news)

    while True:
        try:
            schedule.run_pending()
            time.sleep(1)
        except KeyboardInterrupt:
            print("\n⏹️ 服务已停止")
            break
        except Exception as e:
            print(f"\n❌ 主循环错误: {e}")
            time.sleep(5)

if __name__ == "__main__":
    main()