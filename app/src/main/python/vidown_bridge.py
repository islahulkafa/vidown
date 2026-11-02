import os
import sys
import json
import zipfile
import urllib.request
import traceback

_CONFIG = {
    "ffmpeg_path": None,
    "qjs_path": None,
    "cache_dir": None,
    "update_dir": None,
    "cookies_path": None
}

_CANCELLED_TASKS = set()

def configure(ffmpeg_path, qjs_path, cache_dir, update_dir, cookies_path=None):
    _CONFIG["ffmpeg_path"] = ffmpeg_path
    _CONFIG["qjs_path"] = qjs_path
    _CONFIG["cache_dir"] = cache_dir
    _CONFIG["update_dir"] = update_dir
    _CONFIG["cookies_path"] = cookies_path if (cookies_path and os.path.exists(cookies_path)) else None

    if update_dir and os.path.isdir(update_dir):
        if update_dir not in sys.path:
            sys.path.insert(0, update_dir)

    if ffmpeg_path and os.path.exists(ffmpeg_path):
        ffmpeg_dir = os.path.dirname(ffmpeg_path)
        cur_path = os.environ.get("PATH", "")
        if ffmpeg_dir not in cur_path:
            os.environ["PATH"] = ffmpeg_dir + os.pathsep + cur_path

    return json.dumps({"status": "ok"})

def set_cookies_path(path):
    _CONFIG["cookies_path"] = path if (path and os.path.exists(path)) else None
    return json.dumps({"status": "ok"})

def get_ytdlp_version():
    try:
        import importlib
        v_mod = importlib.import_module("yt_dlp.version")
        return getattr(v_mod, "__version__", None) or getattr(v_mod, "_pkg_version", "unknown")
    except Exception as e:
        return f"unavailable ({e})"

def cancel_task(task_id):
    _CANCELLED_TASKS.add(str(task_id))
    return json.dumps({"status": "ok"})

def probe_info(url):
    try:
        import yt_dlp

        ydl_opts = {
            "quiet": True,
            "no_warnings": True,
            "skip_download": True,
            "extract_flat": "in_playlist",
        }

        if _CONFIG.get("qjs_path") and os.path.exists(_CONFIG["qjs_path"]):
            ydl_opts["js_runtimes"] = {
                "quickjs": {"path": _CONFIG["qjs_path"]}
            }

        if _CONFIG.get("cookies_path") and os.path.exists(_CONFIG["cookies_path"]):
            ydl_opts["cookiefile"] = _CONFIG["cookies_path"]

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                return json.dumps({"status": "error", "error": "No media info returned"})

            is_playlist = info.get("_type") == "playlist" or bool(info.get("entries"))

            if is_playlist:
                raw_entries = info.get("entries")
                if hasattr(raw_entries, "__iter__"):
                    raw_entries = list(raw_entries)
                else:
                    raw_entries = []

                parsed_entries = []
                for idx, entry in enumerate(raw_entries):
                    if not entry:
                        continue
                    entry_id = str(entry.get("id") or "")
                    entry_title = str(entry.get("title") or f"Item {idx + 1}")
                    entry_duration = entry.get("duration") or 0
                    entry_uploader = str(entry.get("uploader") or info.get("uploader") or "")

                    entry_thumb = str(entry.get("thumbnail") or "")
                    if not entry_thumb and entry.get("thumbnails"):
                        thumbs = entry.get("thumbnails")
                        if isinstance(thumbs, list) and thumbs:
                            entry_thumb = str(thumbs[-1].get("url") or "")

                    entry_url = str(entry.get("webpage_url") or entry.get("url") or "")
                    if not entry_url.startswith("http"):
                        if entry_id:
                            if "youtube" in url or "youtu.be" in url:
                                entry_url = f"https://www.youtube.com/watch?v={entry_id}"
                            else:
                                entry_url = str(entry.get("url") or url)
                        else:
                            entry_url = url

                    parsed_entries.append({
                        "id": entry_id,
                        "title": entry_title,
                        "duration": int(entry_duration) if entry_duration else 0,
                        "thumbnail": entry_thumb,
                        "uploader": entry_uploader,
                        "url": entry_url
                    })

                playlist_data = {
                    "id": str(info.get("id") or ""),
                    "title": str(info.get("title") or "Unknown Playlist"),
                    "uploader": str(info.get("uploader") or ""),
                    "item_count": len(parsed_entries),
                    "entries": parsed_entries
                }
                return json.dumps({
                    "status": "ok",
                    "is_playlist": True,
                    "data": playlist_data
                })

            title = info.get("title", "Unknown Title")
            duration = info.get("duration", 0)
            thumbnail = info.get("thumbnail", "")
            uploader = info.get("uploader", "")
            webpage_url = info.get("webpage_url", url)

            subtitles = info.get("subtitles") or {}
            auto_captions = info.get("automatic_captions") or {}
            all_sub_langs = sorted(list(set(list(subtitles.keys()) + list(auto_captions.keys()))))
            has_subtitles = len(all_sub_langs) > 0

            formats = []
            formats.append({
                "id": "b",
                "label": "Best",
                "note": "Fastest, single file without merging",
                "is_audio_only": False,
                "audio_codec": None
            })

            seen_heights = set()
            raw_formats = info.get("formats", [])
            for f in raw_formats:
                height = f.get("height")
                vcodec = f.get("vcodec")
                if height and vcodec != "none" and height not in seen_heights:
                    seen_heights.add(height)
                    formats.append({
                        "id": f"bestvideo[height<={height}]+bestaudio/best[height<={height}]/best",
                        "label": f"{height}p Video",
                        "note": f"Resolution up to {height}p with audio",
                        "is_audio_only": False,
                        "audio_codec": None
                    })

            formats.append({
                "id": "bestaudio/m4a/bestaudio/best",
                "label": "M4A Audio (AAC)",
                "note": "Universal Android compatibility",
                "is_audio_only": True,
                "audio_codec": "m4a"
            })
            formats.append({
                "id": "bestaudio/opus/bestaudio/best",
                "label": "OPUS Audio",
                "note": "High efficiency stream",
                "is_audio_only": True,
                "audio_codec": "opus"
            })
            formats.append({
                "id": "bestaudio/flac/bestaudio/best",
                "label": "FLAC Audio",
                "note": "Lossless CD quality",
                "is_audio_only": True,
                "audio_codec": "flac"
            })
            formats.append({
                "id": "bestaudio/best",
                "label": "Native Audio",
                "note": "Extracted without re-encoding",
                "is_audio_only": True,
                "audio_codec": None
            })

            data = {
                "id": info.get("id", ""),
                "title": title,
                "duration": duration,
                "thumbnail": thumbnail,
                "uploader": uploader,
                "webpage_url": webpage_url,
                "formats": formats,
                "has_subtitles": has_subtitles,
                "available_subtitles": all_sub_langs
            }
            return json.dumps({
                "status": "ok",
                "is_playlist": False,
                "data": data
            })

    except Exception as e:
        return json.dumps({
            "status": "error",
            "error": str(e),
            "traceback": traceback.format_exc()
        })

def download_media(task_id, url, format_id, out_dir, callback_obj, audio_codec=None, embed_subtitles=False, subtitle_lang=None):
    task_id_str = str(task_id)
    if task_id_str in _CANCELLED_TASKS:
        _CANCELLED_TASKS.remove(task_id_str)

    try:
        import yt_dlp

        downloaded_filepath = None

        def progress_hook(d):
            nonlocal downloaded_filepath
            if task_id_str in _CANCELLED_TASKS:
                raise Exception("Download cancelled by user")

            status = d.get("status")
            if status == "downloading":
                total = d.get("total_bytes") or d.get("total_bytes_estimate") or 0
                downloaded = d.get("downloaded_bytes", 0)
                percent = (downloaded / total * 100.0) if total > 0 else 0.0
                speed = d.get("speed") or 0.0
                eta = d.get("eta") or 0

                if callback_obj:
                    try:
                        callback_obj.onProgress(task_id_str, float(percent), float(speed), int(eta), "downloading")
                    except Exception:
                        pass

            elif status == "finished":
                fp = d.get("filename")
                if fp and not fp.endswith(".temp") and not fp.endswith(".part"):
                    downloaded_filepath = fp
                if callback_obj:
                    try:
                        callback_obj.onProgress(task_id_str, 100.0, 0.0, 0, "processing")
                    except Exception:
                        pass

        def postprocessor_hook(d):
            nonlocal downloaded_filepath
            if d.get("status") == "finished":
                info = d.get("info_dict", {})
                fp = info.get("filepath") or d.get("filepath")
                if fp and os.path.exists(fp):
                    downloaded_filepath = fp

        os.makedirs(out_dir, exist_ok=True)
        outtmpl = os.path.join(out_dir, "%(title).100B [%(id)s].%(ext)s")

        ydl_opts = {
            "format": format_id or "b",
            "outtmpl": outtmpl,
            "progress_hooks": [progress_hook],
            "postprocessor_hooks": [postprocessor_hook],
            "quiet": True,
            "no_warnings": True,
            "noplaylist": True,
        }

        has_ffmpeg = bool(_CONFIG.get("ffmpeg_path") and os.path.exists(_CONFIG["ffmpeg_path"]))
        if has_ffmpeg:
            ydl_opts["ffmpeg_location"] = _CONFIG["ffmpeg_path"]

            postprocessors = []

            if audio_codec:
                postprocessors.append({
                    "key": "FFmpegExtractAudio",
                    "preferredcodec": audio_codec,
                    "preferredquality": "0",
                })

            if embed_subtitles:
                ydl_opts["writesubtitles"] = True
                ydl_opts["writeautomaticsub"] = True
                if subtitle_lang:
                    ydl_opts["subtitleslangs"] = [subtitle_lang]
                else:
                    ydl_opts["subtitleslangs"] = ["en"]
                postprocessors.append({
                    "key": "FFmpegEmbedSubtitle",
                    "already_have_subtitle": False,
                })
                ydl_opts["merge_output_format"] = "mp4"

            ydl_opts["writethumbnail"] = True
            postprocessors.append({
                "key": "FFmpegThumbnailsConvertor",
                "format": "jpg",
            })
            postprocessors.append({
                "key": "FFmpegMetadata",
                "add_metadata": True,
            })
            postprocessors.append({
                "key": "EmbedThumbnail",
                "already_have_thumbnail": False,
            })

            ydl_opts["postprocessors"] = postprocessors

        if _CONFIG.get("qjs_path") and os.path.exists(_CONFIG["qjs_path"]):
            ydl_opts["js_runtimes"] = {
                "quickjs": {"path": _CONFIG["qjs_path"]}
            }

        if _CONFIG.get("cookies_path") and os.path.exists(_CONFIG["cookies_path"]):
            ydl_opts["cookiefile"] = _CONFIG["cookies_path"]

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ret_code = ydl.download([url])
            if ret_code != 0:
                return json.dumps({"status": "error", "error": f"yt-dlp exited with code {ret_code}"})

        if not downloaded_filepath or not os.path.exists(downloaded_filepath):
            files = [
                os.path.join(out_dir, f) for f in os.listdir(out_dir)
                if not f.endswith(".part")
                and not f.endswith(".ytdl")
                and not f.endswith(".temp")
                and not f.endswith(".webp")
                and not f.endswith(".jpg")
                and not f.endswith(".png")
                and not f.endswith(".vtt")
                and not f.endswith(".srt")
            ]
            if files:
                downloaded_filepath = max(files, key=os.path.getmtime)

        if downloaded_filepath and os.path.exists(downloaded_filepath):
            return json.dumps({
                "status": "ok",
                "filepath": downloaded_filepath,
                "filename": os.path.basename(downloaded_filepath),
                "filesize": os.path.getsize(downloaded_filepath)
            })
        else:
            return json.dumps({"status": "error", "error": "Output file not found after download completion"})

    except Exception as e:
        is_cancelled = task_id_str in _CANCELLED_TASKS or "cancelled" in str(e).lower()
        if task_id_str in _CANCELLED_TASKS:
            _CANCELLED_TASKS.remove(task_id_str)

        return json.dumps({
            "status": "cancelled" if is_cancelled else "error",
            "error": str(e)
        })

def update_ytdlp():
    update_dir = _CONFIG.get("update_dir")
    if not update_dir:
        return json.dumps({"status": "error", "error": "Update directory not configured"})

    try:
        os.makedirs(update_dir, exist_ok=True)

        req = urllib.request.Request(
            "https://pypi.org/pypi/yt-dlp/json",
            headers={"User-Agent": "Vidown/1.0"}
        )
        with urllib.request.urlopen(req, timeout=15) as response:
            data = json.loads(response.read().decode("utf-8"))

        latest_version = data["info"]["version"]
        cur_version = get_ytdlp_version()

        if cur_version and latest_version == cur_version:
            return json.dumps({
                "status": "up_to_date",
                "version": cur_version,
                "message": f"yt-dlp is already at the latest version ({cur_version})"
            })

        wheel_url = None
        for release_file in data.get("urls", []):
            filename = release_file.get("filename", "")
            if filename.endswith("-py3-none-any.whl") or filename.endswith("-py2.py3-none-any.whl"):
                wheel_url = release_file.get("url")
                break

        if not wheel_url:
            return json.dumps({"status": "error", "error": "No compatible wheel found on PyPI"})

        wheel_path = os.path.join(update_dir, "yt_dlp_latest.whl")
        urllib.request.urlretrieve(wheel_url, wheel_path)

        with zipfile.ZipFile(wheel_path, "r") as zip_ref:
            zip_ref.extractall(update_dir)

        if os.path.exists(wheel_path):
            os.remove(wheel_path)

        if update_dir not in sys.path:
            sys.path.insert(0, update_dir)

        import importlib
        for mod_name in list(sys.modules.keys()):
            if mod_name == "yt_dlp" or mod_name.startswith("yt_dlp."):
                del sys.modules[mod_name]
        importlib.invalidate_caches()

        new_version = get_ytdlp_version()
        return json.dumps({
            "status": "ok",
            "previous_version": cur_version,
            "new_version": new_version
        })

    except Exception as e:
        return json.dumps({
            "status": "error",
            "error": str(e),
            "traceback": traceback.format_exc()
        })
