import akshare as ak
import pandas as pd
from sqlalchemy import create_engine, text
import time
import schedule
import datetime
import random
import sys

# ================= 配置区域 =================
DB_URL = "postgresql+psycopg2://postgres:030314@localhost:5432/fincoach"

# ================= 数据库连接 =================
try:
    engine = create_engine(DB_URL, pool_recycle=3600, pool_pre_ping=True)
    print(f"✅ 数据库连接成功")
except Exception as e:
    print(f"❌ 数据库连接失败: {e}")
    sys.exit(1)

# ================= 核心功能: 仿真模式 (The Safety Net) =================

def simulate_price_fluctuation():
    """
    仿真模式：当无法从互联网获取数据时，对现有价格进行微调。
    波动范围：±0.5%
    """
    try:
        with engine.begin() as conn:
            # 1. 查出所有股票的当前价格
            result = conn.execute(text("SELECT code, current_price FROM market_security"))
            rows = result.fetchall()
            
            updated_count = 0
            for row in rows:
                code = row[0]
                old_price = float(row[1]) if row[1] else 10.0 # 默认兜底价
                
                # 2. 生成随机波动 (0.995 ~ 1.005)
                fluctuation = random.uniform(0.995, 1.005)
                new_price = old_price * fluctuation
                
                # 3. 计算新的模拟涨跌幅 (相对于昨日收盘价，这里简化为相对于上一刻的变动展示)
                # 在真实仿真中，通常需要记录昨日收盘价。这里为了演示效果，直接模拟一个涨跌幅
                simulated_change = (fluctuation - 1) * 100 * 10 # 放大一点波动显示
                
                # 4. 更新数据库
                conn.execute(text("""
                    UPDATE market_security 
                    SET current_price = :p, change_percent = :c 
                    WHERE code = :code
                """), {'p': new_price, 'c': simulated_change, 'code': code})
                updated_count += 1
                
        print(f" -> 🎲 [仿真模式] 已模拟 {updated_count} 只股票的波动")
        return True
    except Exception as e:
        print(f" -> ❌ 仿真失败: {e}")
        return False

# ================= 核心功能: 真实行情 (Real World) =================

def fetch_real_market():
    """尝试获取真实行情，失败则返回 False"""
    try:
        # 优先尝试新浪源 (Sina)，速度快且封禁概率低
        df = ak.stock_zh_a_spot() 
        # 映射列名
        df = df.rename(columns={'code': 'code', 'trade': 'price', 'changepercent': 'change'})
        return df
    except:
        return None

def update_market_logic():
    """主逻辑：真数据优先 -> 仿真兜底"""
    timestamp = datetime.datetime.now().strftime('%H:%M:%S')
    print(f"[{timestamp}] 更新行情...", end="")

    # --- 1. 尝试获取真实数据 ---
    df_real = fetch_real_market()
    
    if df_real is not None and not df_real.empty:
        # 真实数据入库
        try:
            with engine.connect() as conn:
                # 获取关注列表
                codes = [row[0] for row in conn.execute(text("SELECT code FROM market_security")).fetchall()]
                
            # 筛选
            df_target = df_real[df_real['code'].isin(codes)]
            
            if not df_target.empty:
                with engine.begin() as conn:
                    for _, row in df_target.iterrows():
                        conn.execute(text("""
                            UPDATE market_security 
                            SET current_price = :p, change_percent = :c 
                            WHERE code = :code
                        """), {'p': row['price'], 'c': row['change'], 'code': row['code']})
                print(f" -> ✅ [真实数据] 更新成功")
                return # 成功退出
        except Exception as e:
            print(f" (入库错误: {e})", end=" ")
            
    # --- 2. 降级到仿真模式 ---
    print(" -> ⚠️ 网络/接口异常，切换仿真...", end="")
    simulate_price_fluctuation()

# ================= 辅助功能: 新闻更新 =================

def update_news_safe():
    try:
        # 使用最稳定的金十数据
        df = ak.js_news(timestamp=datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
        if not df.empty:
            with engine.begin() as conn:
                for _, row in df.head(3).iterrows():
                    content = row['content']
                    # 简单去重写入...
                    exists = conn.execute(text("SELECT count(*) FROM financial_news WHERE content=:c"), {'c':content}).scalar()
                    if exists == 0:
                        conn.execute(text("INSERT INTO financial_news (title, content, source, publish_time) VALUES (:t, :c, '金十', NOW())"),
                                     {'t': content[:30], 'c': content})
            print(f"\n[{datetime.datetime.now().strftime('%H:%M:%S')}] 新闻更新完成")
    except:
        pass # 新闻失败完全忽略，不影响主线程

# ================= 启动入口 =================

if __name__ == "__main__":
    print("🚀 FinCoach 终极行情服务 (双模版) 已启动")
    
    # 立即执行一次
    update_market_logic()
    
    # 定时任务
    schedule.every(5).seconds.do(update_market_logic)
    schedule.every(2).minutes.do(update_news_safe)
    
    while True:
        schedule.run_pending()
        time.sleep(1)
