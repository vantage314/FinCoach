import json
from datetime import date


def main():
    today = date.today().isoformat()
    assets = [
        ("SPY.US", 521.35),
        ("AGG.US", 97.42),
        ("CASH", 1.00),
    ]
    for asset_key, price in assets:
        line = {"assetKey": asset_key, "date": today, "price": price}
        print(json.dumps(line, ensure_ascii=False))


if __name__ == "__main__":
    main()
