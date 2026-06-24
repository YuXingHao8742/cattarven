import json
import sys

def analyze(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        print(f"=== File: {filepath} ===")
        print("Root keys:", list(data.keys()))
        if "data" in data:
            print("Data keys:", list(data["data"].keys()))
            if "character_book" in data["data"]:
                book = data["data"]["character_book"]
                if "entries" in book:
                    entries = book["entries"]
                    print("Entries type:", type(entries))
                    if isinstance(entries, list) and len(entries) > 0:
                        print("First entry keys:", list(entries[0].keys()))
                    elif isinstance(entries, dict) and len(entries) > 0:
                        first_key = list(entries.keys())[0]
                        print("First entry keys:", list(entries[first_key].keys()))
    except Exception as e:
        print("Error:", e)

analyze('c:\\Users\\13779\\Desktop\\cattarven\\app\\src\\main\\java\\cat\\tarven\\data\\model\\潜意识修改·灵.json')
analyze('c:\\Users\\13779\\Desktop\\cattarven\\app\\src\\main\\java\\cat\\tarven\\data\\model\\端心澜.json')
