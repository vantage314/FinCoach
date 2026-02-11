import requests
import time
import schedule
import datetime
import sys
from sqlalchemy import create_engine, text

# ================= 配置区域 =================
# 数据库连接 (请确认密码是否正确)
DB_URL = "mysql+pymysql://root:030314@localhost:3306/fincoach?charset=utf8mb4"

# ================= 数据库连接 =================
try:
    engine = create_engine(DB_URL, pool_recycle=3600, pool_pre_ping=True)
    print(f"✅ 数据库连接成功")
except Exception as e:
    print(f"❌ 数据库连接失败: {e}")
    sys.exit(1)

# ================= 核心工具: 交易所前缀判断 =================
def get_stock_with_prefix(code):
    """
    根据代码判断是上海(sh)还是深圳(sz)
    新浪/腾讯接口需要: sh600519, sz000001
    """
    code = str(code).strip()
    if code.startswith('6'):
        return f"sh{code}"
    elif code.startswith('0') or code.startswith('3'):
        return f"sz{code}"
    elif code.startswith('4') or code.startswith('8'):
        return f"bj{code}" # 北交所
    else:
        return f"sh{code}" # 默认尝试sh

# ================= 数据源 A: 新浪财经 (Sina) =================
def fetch_sina_data(stock_list):
    """
    请求新浪接口 - 完整解析版 (含五档盘口)
    格式: http://hq.sinajs.cn/list=sh600519,sz000001
    新浪字段索引:
      0:名称  1:今开  2:昨收  3:当前价  4:最高  5:最低
      8:成交量(股)  9:成交额(元)
      10:买一量  11:买一价  12:买二量  13:买二价  14:买三量  15:买三价
      16:买四量  17:买四价  18:买五量  19:买五价
      20:卖一量  21:卖一价  22:卖二量  23:卖二价  24:卖三量  25:卖三价
      26:卖四量  27:卖四价  28:卖五量  29:卖五价
    """
    if not stock_list: return {}
    
    codes_str = ",".join(stock_list)
    url = f"http://hq.sinajs.cn/list={codes_str}"
    headers = {'Referer': 'https://finance.sina.com.cn'}
    
    try:
        resp = requests.get(url, headers=headers, timeout=5)
        if resp.status_code != 200:
            return None
            
        data_map = {}
        lines = resp.text.split('\n')
        for line in lines:
            if '="' in line:
                try:
                    left, right = line.split('="')
                    code_with_prefix = left.split('_')[-1]
                    raw_code = code_with_prefix[2:]
                    
                    data_str = right.strip('";')
                    if not data_str: continue
                    
                    fields = data_str.split(',')
                    if len(fields) < 30: continue
                    
                    # ===== 基本行情 =====
                    name = fields[0]
                    open_price = float(fields[1])
                    pre_close = float(fields[2])
                    current_price = float(fields[3])
                    high_price = float(fields[4])
                    low_price = float(fields[5])
                    volume = int(float(fields[8]))
                    turnover = float(fields[9])
                    
                    # ===== 五档盘口 =====
                    bid1_vol = int(float(fields[10])); bid1_price = float(fields[11])
                    bid2_vol = int(float(fields[12])); bid2_price = float(fields[13])
                    bid3_vol = int(float(fields[14])); bid3_price = float(fields[15])
                    bid4_vol = int(float(fields[16])); bid4_price = float(fields[17])
                    bid5_vol = int(float(fields[18])); bid5_price = float(fields[19])
                    ask1_vol = int(float(fields[20])); ask1_price = float(fields[21])
                    ask2_vol = int(float(fields[22])); ask2_price = float(fields[23])
                    ask3_vol = int(float(fields[24])); ask3_price = float(fields[25])
                    ask4_vol = int(float(fields[26])); ask4_price = float(fields[27])
                    ask5_vol = int(float(fields[28])); ask5_price = float(fields[29])
                    
                    # 计算涨跌幅
                    if pre_close > 0:
                        change_percent = ((current_price - pre_close) / pre_close) * 100
                    else:
                        change_percent = 0.0
                        
                    data_map[raw_code] = {
                        'price': current_price,
                        'change': round(change_percent, 2),
                        'name': name,
                        'open': open_price,
                        'high': high_price,
                        'low': low_price,
                        'volume': volume,
                        'turnover': round(turnover, 2),
                        # 买盘
                        'bid1_price': bid1_price, 'bid1_vol': bid1_vol,
                        'bid2_price': bid2_price, 'bid2_vol': bid2_vol,
                        'bid3_price': bid3_price, 'bid3_vol': bid3_vol,
                        'bid4_price': bid4_price, 'bid4_vol': bid4_vol,
                        'bid5_price': bid5_price, 'bid5_vol': bid5_vol,
                        # 卖盘
                        'ask1_price': ask1_price, 'ask1_vol': ask1_vol,
                        'ask2_price': ask2_price, 'ask2_vol': ask2_vol,
                        'ask3_price': ask3_price, 'ask3_vol': ask3_vol,
                        'ask4_price': ask4_price, 'ask4_vol': ask4_vol,
                        'ask5_price': ask5_price, 'ask5_vol': ask5_vol,
                    }
                except:
                    continue
        return data_map
    except Exception as e:
        print(f"⚠️ 新浪接口异常: {e}")
        return None

# ================= 数据源 B: 腾讯财经 (Tencent) =================
def fetch_tencent_data(stock_list):
    """
    请求腾讯接口 (备用)
    格式: http://qt.gtimg.cn/q=sh600519,sz000001
    """
    if not stock_list: return {}
    
    codes_str = ",".join(stock_list)
    url = f"http://qt.gtimg.cn/q={codes_str}"
    
    try:
        resp = requests.get(url, timeout=3)
        if resp.status_code != 200:
            return None
            
        # v_sh600519="51~贵州茅台~600519~1710.00~..."
        data_map = {}
        lines = resp.text.split('\n')
        for line in lines:
            if '="' in line:
                try:
                    left, right = line.split('="')
                    raw_code = left.split('_')[-1][2:] # 600519
                    
                    data_str = right.strip('";')
                    fields = data_str.split('~')
                    if len(fields) < 30: continue
                    
                    # 腾讯字段: 3:当前价, 32:涨跌幅
                    current_price = float(fields[3])
                    change_percent = float(fields[32])
                    
                    data_map[raw_code] = {
                        'price': current_price,
                        'change': change_percent
                    }
                except:
                    continue
        return data_map
    except Exception as e:
        print(f"⚠️ 腾讯接口异常: {e}")
        return None

# ================= 主逻辑 =================
def update_market_real():
    timestamp = datetime.datetime.now().strftime('%H:%M:%S')
    print(f"[{timestamp}] 正在获取真实行情...", end="")
    
    # 1. 从数据库获取代码
    try:
        with engine.connect() as conn:
            # 兼容大小写
            result = conn.execute(text("SELECT code FROM market_security"))
            db_codes = [str(row[0]) for row in result.fetchall()]
    except Exception as e:
        print(f" -> ❌ 读库失败: {e}")
        return

    if not db_codes:
        print(" -> 数据库无股票")
        return

    # 2. 转换代码格式 (加前缀 sh/sz)
    request_list = [get_stock_with_prefix(c) for c in db_codes]
    
    # 3. 尝试获取数据 (优先新浪，失败切腾讯)
    source_name = "新浪"
    market_data = fetch_sina_data(request_list)
    
    if market_data is None:
        print(" (新浪无响应，切换腾讯)...", end="")
        source_name = "腾讯"
        market_data = fetch_tencent_data(request_list)
        
    if market_data is None:
        print(" -> ❌ 所有接口均连接失败 (检查网络)")
        return

    if not market_data:
        print(f" -> {source_name} 返回空数据 (可能是休市或代码错误)")
        return

    # 4. 更新数据库 (含五档盘口)
    try:
        updated_count = 0
        with engine.begin() as conn:
            for code, data in market_data.items():
                if data['price'] > 0:
                    conn.execute(text("""
                        UPDATE market_security 
                        SET current_price = :p, change_percent = :c,
                            open_price = :open, high_price = :high, low_price = :low,
                            volume = :vol, turnover = :turnover,
                            bid1_price = :b1p, bid1_vol = :b1v,
                            bid2_price = :b2p, bid2_vol = :b2v,
                            bid3_price = :b3p, bid3_vol = :b3v,
                            bid4_price = :b4p, bid4_vol = :b4v,
                            bid5_price = :b5p, bid5_vol = :b5v,
                            ask1_price = :a1p, ask1_vol = :a1v,
                            ask2_price = :a2p, ask2_vol = :a2v,
                            ask3_price = :a3p, ask3_vol = :a3v,
                            ask4_price = :a4p, ask4_vol = :a4v,
                            ask5_price = :a5p, ask5_vol = :a5v
                        WHERE code = :code
                    """), {
                        'p': data['price'], 'c': data['change'],
                        'open': data.get('open', 0), 'high': data.get('high', 0),
                        'low': data.get('low', 0), 'vol': data.get('volume', 0),
                        'turnover': data.get('turnover', 0),
                        'b1p': data.get('bid1_price', 0), 'b1v': data.get('bid1_vol', 0),
                        'b2p': data.get('bid2_price', 0), 'b2v': data.get('bid2_vol', 0),
                        'b3p': data.get('bid3_price', 0), 'b3v': data.get('bid3_vol', 0),
                        'b4p': data.get('bid4_price', 0), 'b4v': data.get('bid4_vol', 0),
                        'b5p': data.get('bid5_price', 0), 'b5v': data.get('bid5_vol', 0),
                        'a1p': data.get('ask1_price', 0), 'a1v': data.get('ask1_vol', 0),
                        'a2p': data.get('ask2_price', 0), 'a2v': data.get('ask2_vol', 0),
                        'a3p': data.get('ask3_price', 0), 'a3v': data.get('ask3_vol', 0),
                        'a4p': data.get('ask4_price', 0), 'a4v': data.get('ask4_vol', 0),
                        'a5p': data.get('ask5_price', 0), 'a5v': data.get('ask5_vol', 0),
                        'code': code
                    })
                    updated_count += 1
        
        print(f" -> ✅ ({source_name}) 更新 {updated_count} 只股票 [含盘口五档]")
    
    except Exception as e:
        print(f" -> ❌ 写库失败: {e}")

# ================= 辅助功能: 新闻获取 (akshare) =================

def update_news_real():
    """
    获取 7x24 财经快讯，写入 financial_news 表。
    优先金十数据(js_news)，失败则尝试财联社(stock_telegraph_cls)。
    新闻失败绝不中断主程序。
    """
    timestamp = datetime.datetime.now().strftime('%H:%M:%S')
    try:
        import akshare as ak
    except ImportError:
        # akshare 未安装则跳过新闻功能
        return

    df_news = None
    source = ""

    # 来源 A: 金十数据
    try:
        df_news = ak.js_news(timestamp=datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
        source = "金十数据"
    except:
        pass

    # 来源 B: 财联社
    if df_news is None or (hasattr(df_news, 'empty') and df_news.empty):
        try:
            df_news = ak.stock_telegraph_cls(symbol="财联社电报")
            source = "财联社"
        except:
            pass

    if df_news is None or (hasattr(df_news, 'empty') and df_news.empty):
        # 所有新闻源失败，静默跳过
        return

    try:
        count = 0
        with engine.begin() as conn:
            for _, row in df_news.head(5).iterrows():
                # 智能识别列名（金十用 content，财联社用 标题/内容）
                content = str(row.get('content', row.get('内容', '')))
                title = str(row.get('title', row.get('标题', '')))

                if not content:
                    continue
                if not title:
                    title = content[:30] + '...'

                # 查重
                exists = conn.execute(
                    text("SELECT COUNT(*) FROM financial_news WHERE content = :c"),
                    {'c': content}
                ).scalar()

                if exists == 0:
                    # 兼容 financial_news 表是否有 summary 字段
                    try:
                        conn.execute(text("""
                            INSERT INTO financial_news (title, summary, content, source, publish_time)
                            VALUES (:t, :s, :c, :src, NOW())
                        """), {
                            't': title[:200],
                            's': content[:100],
                            'c': content,
                            'src': source
                        })
                    except:
                        # 如果没有 summary 字段
                        conn.execute(text("""
                            INSERT INTO financial_news (title, content, source, publish_time)
                            VALUES (:t, :c, :src, NOW())
                        """), {
                            't': title[:200],
                            'c': content,
                            'src': source
                        })
                    count += 1

        if count > 0:
            print(f"\n[{timestamp}] 📰 新闻更新: ({source}) 新增 {count} 条")
    except Exception as e:
        # 新闻处理失败，静默忽略
        pass

# ================= 启动 =================
if __name__ == "__main__":
    print("\n🚀 FinCoach 真实行情直连服务 v3.0")
    print("---------------------------------------")
    print("数据源: 新浪财经(主) / 腾讯财经(备)")
    print("新闻源: 金十数据(主) / 财联社(备)")
    print("行情字段: 现价/今开/最高/最低/成交量/成交额")
    print("更新频率: 行情 3秒/次 | 新闻 60秒/次")
    print("---------------------------------------")
    
    update_market_real() # 立即执行行情
    update_news_real()   # 立即执行新闻
    
    # 定时任务
    schedule.every(3).seconds.do(update_market_real)
    schedule.every(60).seconds.do(update_news_real)
    
    while True:
        schedule.run_pending()
        time.sleep(1)
