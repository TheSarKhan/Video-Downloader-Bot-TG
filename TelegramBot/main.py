from telegram import Update
from telegram.ext import Application, CommandHandler, MessageHandler, filters, ContextTypes
import requests
from io import BytesIO
import time

TOKEN = "7997893093:AAEVCynAlmCsvbVJCX28NWw5V68xaQYYrUM"
API_BASE = "http://localhost:8080/api"

# Mesaj geldiğinde çalışacak handler
async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    url = update.message.text.strip()  # Kullanıcı linki gönderdi
    await update.message.reply_text("Datanı yükləyirəm... ⏳ Hazır ol, yüngül səbrli olginən server ucuz serverdi")

    try:
        # 1️⃣ Task oluştur
        payload = {
            "url": url,
            "proxy": "",
            "cookies": "",
            "format": "mp4",
            "noWatermark": True,
            "audioOnly": False,
            "useCache": False
        }
        r = requests.post(f"{API_BASE}/download", json=payload)
        r.raise_for_status()
        data = r.json()
        video_id = data.get("videoId")
        task_status = data.get("status")
        await update.message.reply_text(f"Endirilmə başladı...")

        # 2️⃣ Kısa bekleme, video hazır olana kadar
        time.sleep(2)  # backend’in videoyu oluşturması için 2 saniye bekle

        # 3️⃣ Dosyayı indir
        file_url = f"{API_BASE}/files/{video_id}.mp4"
        resp = requests.get(file_url, stream=True)
        resp.raise_for_status()

        video_data = BytesIO(resp.content)
        video_data.name = f"{video_id}.mp4"

        # 4️⃣ Telegram'a gönder
        await update.message.reply_video(video_data, caption="🎬 Alginən")

    except Exception as e:
        await update.message.reply_text(f"Xəta: {e} 😅 Erroru at qaqava +994 50 201 71 64 !")

def main():
    app = Application.builder().token(TOKEN).build()

    # /start komutu
    async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
        await update.message.reply_text("Salamlar! Linki göndər, qardaşun yükləsin 😎")

    app.add_handler(CommandHandler("start", start))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))

    print("Bot işləyir...  ")
    app.run_polling()

if __name__ == "__main__":
    main()
