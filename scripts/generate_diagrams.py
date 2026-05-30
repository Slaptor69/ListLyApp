from __future__ import annotations

import math
import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "diagrams"

BG = "#ffffff"
BOX_FILL = "#f4f0ff"
BOX_BORDER = "#8268e8"
ACCENT_FILL = "#eef7ff"
ACCENT_BORDER = "#4a90c2"
GREEN_FILL = "#effaf1"
GREEN_BORDER = "#4c9f5d"
YELLOW_FILL = "#fff8df"
YELLOW_BORDER = "#c49b28"
LINE = "#333333"
TEXT = "#1b1b1f"
MUTED = "#5f5f66"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        Path("C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"),
        Path("C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf"),
        Path("C:/Windows/Fonts/tahoma.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size=size)
    return ImageFont.load_default()


TITLE_FONT = font(36, True)
BOX_FONT = font(20)
SMALL_FONT = font(17)
CAPTION_FONT = font(18, True)


def wrap_text(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.ImageFont, width: int) -> list[str]:
    words = text.replace("<br/>", "\n").splitlines()
    result: list[str] = []
    for line in words:
        current = ""
        for word in line.split():
            candidate = f"{current} {word}".strip()
            if draw.textbbox((0, 0), candidate, font=fnt)[2] <= width:
                current = candidate
            else:
                if current:
                    result.append(current)
                if draw.textbbox((0, 0), word, font=fnt)[2] > width:
                    result.extend(textwrap.wrap(word, width=max(8, width // 12)))
                    current = ""
                else:
                    current = word
        if current:
            result.append(current)
    return result


def centered_text(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    fnt: ImageFont.ImageFont = BOX_FONT,
    color: str = TEXT,
) -> None:
    x1, y1, x2, y2 = box
    lines = wrap_text(draw, text, fnt, x2 - x1 - 24)
    heights = [draw.textbbox((0, 0), line, font=fnt)[3] - draw.textbbox((0, 0), line, font=fnt)[1] for line in lines]
    total_h = sum(heights) + max(0, len(lines) - 1) * 6
    y = y1 + ((y2 - y1) - total_h) / 2
    for line, h in zip(lines, heights):
        bbox = draw.textbbox((0, 0), line, font=fnt)
        x = x1 + ((x2 - x1) - (bbox[2] - bbox[0])) / 2
        draw.text((x, y), line, font=fnt, fill=color)
        y += h + 6


def box(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    text: str,
    fill: str = BOX_FILL,
    outline: str = BOX_BORDER,
    radius: int = 8,
    fnt: ImageFont.ImageFont = BOX_FONT,
) -> None:
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=2)
    centered_text(draw, xy, text, fnt)


def note(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    text: str,
    fill: str = YELLOW_FILL,
    outline: str = YELLOW_BORDER,
) -> None:
    box(draw, xy, text, fill=fill, outline=outline, radius=8, fnt=SMALL_FONT)


def cylinder(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], text: str) -> None:
    x1, y1, x2, y2 = xy
    h = 24
    draw.rectangle((x1, y1 + h // 2, x2, y2 - h // 2), fill=GREEN_FILL, outline=GREEN_BORDER, width=2)
    draw.ellipse((x1, y1, x2, y1 + h), fill=GREEN_FILL, outline=GREEN_BORDER, width=2)
    draw.arc((x1, y2 - h, x2, y2), 0, 180, fill=GREEN_BORDER, width=2)
    draw.line((x1, y1 + h // 2, x1, y2 - h // 2), fill=GREEN_BORDER, width=2)
    draw.line((x2, y1 + h // 2, x2, y2 - h // 2), fill=GREEN_BORDER, width=2)
    centered_text(draw, xy, text, SMALL_FONT)


def arrowhead(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int]) -> None:
    sx, sy = start
    ex, ey = end
    angle = math.atan2(ey - sy, ex - sx)
    size = 12
    p1 = (ex, ey)
    p2 = (ex - size * math.cos(angle - math.pi / 6), ey - size * math.sin(angle - math.pi / 6))
    p3 = (ex - size * math.cos(angle + math.pi / 6), ey - size * math.sin(angle + math.pi / 6))
    draw.polygon([p1, p2, p3], fill=LINE)


def arrow(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], label: str | None = None) -> None:
    draw.line(points, fill=LINE, width=2, joint="curve")
    arrowhead(draw, points[-2], points[-1])
    if label:
        mx = (points[0][0] + points[-1][0]) // 2
        my = (points[0][1] + points[-1][1]) // 2
        draw.text((mx + 6, my - 20), label, font=SMALL_FONT, fill=MUTED)


def title(draw: ImageDraw.ImageDraw, text: str, width: int) -> None:
    bbox = draw.textbbox((0, 0), text, font=TITLE_FONT)
    draw.text(((width - (bbox[2] - bbox[0])) / 2, 26), text, font=TITLE_FONT, fill=TEXT)


def save(name: str, size: tuple[int, int], painter) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGB", size, BG)
    draw = ImageDraw.Draw(img)
    painter(draw)
    img.save(OUT / f"{name}.png", quality=95)


def architecture() -> None:
    def paint(d: ImageDraw.ImageDraw) -> None:
        title(d, "Схема 1. Общая архитектура Android-клиента Listly", 2000)
        boxes = {
            "user": (50, 390, 230, 480),
            "ui": (330, 340, 560, 520),
            "nav": (650, 210, 910, 310),
            "store": (650, 430, 910, 550),
            "reducer": (1030, 245, 1270, 345),
            "actor": (1030, 505, 1270, 605),
            "interactor": (1380, 420, 1620, 520),
            "repo": (1380, 640, 1620, 790),
            "okhttp": (1740, 505, 1940, 605),
            "backend": (1740, 700, 1940, 820),
            "prefs": (980, 685, 1240, 780),
            "encrypted": (980, 795, 1240, 890),
        }
        box(d, boxes["user"], "Пользователь", ACCENT_FILL, ACCENT_BORDER)
        box(d, boxes["ui"], "Jetpack Compose UI\nAuth, Catalog,\nReadlist, Settings", BOX_FILL, BOX_BORDER)
        box(d, boxes["nav"], "Navigation 3\nglobal backstack\n+ bottom tabs", ACCENT_FILL, ACCENT_BORDER)
        box(d, boxes["store"], "Elmslie Store\nState / Event /\nCommand / Effect", BOX_FILL, BOX_BORDER)
        box(d, boxes["reducer"], "Reducer\nчистое изменение\nState", BOX_FILL, BOX_BORDER)
        box(d, boxes["actor"], "Actor\nасинхронные\nкоманды", BOX_FILL, BOX_BORDER)
        box(d, boxes["interactor"], "MediaItemInteractor\nбизнес-операции\nreadlist", BOX_FILL, BOX_BORDER)
        box(d, boxes["repo"], "Repositories\nAuthRepository\nMediaItemRepository\nThemeRepository", BOX_FILL, BOX_BORDER)
        box(d, boxes["okhttp"], "OkHttp\nJSON parsing", GREEN_FILL, GREEN_BORDER)
        cylinder(d, boxes["backend"], "Backend API\nauth, media,\nfolders, user-media")
        cylinder(d, boxes["encrypted"], "Encrypted\nSharedPreferences\nJWT token")
        cylinder(d, boxes["prefs"], "SharedPreferences\nlogin, theme,\nbaseUrl")

        arrow(d, [(230, 435), (330, 435)])
        arrow(d, [(560, 380), (650, 260)])
        arrow(d, [(560, 455), (650, 490)])
        arrow(d, [(910, 455), (1030, 295)])
        arrow(d, [(910, 510), (1030, 555)])
        arrow(d, [(1270, 545), (1340, 545), (1340, 470), (1380, 470)])
        arrow(d, [(1270, 585), (1320, 585), (1320, 715), (1380, 715)])
        arrow(d, [(1620, 700), (1690, 700), (1690, 555), (1740, 555)])
        arrow(d, [(1840, 605), (1840, 700)])
        arrow(d, [(1380, 690), (1240, 730)])
        arrow(d, [(1380, 760), (1240, 840)])
        arrow(d, [(1270, 530), (1380, 500)], "данные")
        note(d, (650, 640, 910, 760), "UI не ходит в сеть напрямую:\nвся внешняя работа проходит\nчерез Actor и Repository.")
    save("01_architecture", (2000, 900), paint)


def navigation() -> None:
    def paint(d: ImageDraw.ImageDraw) -> None:
        title(d, "Схема 2. Навигация приложения", 1600)
        box(d, (650, 110, 950, 190), "Запуск MainActivity", ACCENT_FILL, ACCENT_BORDER)
        box(d, (650, 250, 950, 340), "Есть сохраненный JWT?", YELLOW_FILL, YELLOW_BORDER)
        box(d, (250, 430, 530, 540), "AuthScreen\nвход / регистрация")
        box(d, (850, 430, 1150, 540), "MainScreenEntry\nнижняя навигация")
        box(d, (460, 660, 700, 750), "Каталог")
        box(d, (730, 660, 970, 750), "Мои списки")
        box(d, (1000, 660, 1240, 750), "Настройки")
        box(d, (250, 820, 530, 910), "MediaItemScreen\nдетальная карточка")
        box(d, (1030, 820, 1310, 910), "FolderManagementScreen\nуправление папками")
        arrow(d, [(800, 190), (800, 250)])
        arrow(d, [(650, 295), (560, 295), (560, 485), (530, 485)], "нет")
        arrow(d, [(950, 295), (1030, 295), (1030, 430)], "да")
        arrow(d, [(530, 485), (850, 485)], "успешный вход")
        arrow(d, [(1000, 540), (580, 660)])
        arrow(d, [(1000, 540), (850, 660)])
        arrow(d, [(1000, 540), (1120, 660)])
        arrow(d, [(580, 750), (390, 820)], "клик по карточке")
        arrow(d, [(390, 820), (580, 750)], "назад")
        arrow(d, [(1120, 750), (1170, 820)], "папки")
        arrow(d, [(1170, 820), (1120, 750)], "назад")
        arrow(d, [(1120, 660), (1120, 610), (390, 610), (390, 540)], "авторизация из настроек")
    save("02_navigation", (1600, 980), paint)


def elm_cycle() -> None:
    def paint(d: ImageDraw.ImageDraw) -> None:
        title(d, "Схема 3. ELM-цикл экрана", 1700)
        x_positions = [90, 350, 620, 890, 1160, 1430]
        labels = ["Пользователь", "Compose Screen", "ElmStore", "Reducer", "Actor", "Repository /\nInteractor"]
        for x, label in zip(x_positions, labels):
            box(d, (x, 130, x + 210, 220), label, ACCENT_FILL if x in [90, 350] else BOX_FILL, ACCENT_BORDER if x in [90, 350] else BOX_BORDER)
            d.line((x + 105, 220, x + 105, 820), fill="#dddddd", width=2)
        steps = [
            (195, 455, 260, "действие"),
            (455, 725, 295, "Ui Event"),
            (725, 995, 360, "reduce(event, state)"),
            (995, 725, 425, "State + Command/Effect"),
            (725, 455, 490, "State для отрисовки"),
            (725, 1265, 565, "Command"),
            (1265, 1535, 630, "асинхронная операция"),
            (1535, 1265, 695, "результат"),
            (1265, 725, 760, "Internal Event"),
        ]
        for x1, x2, y, label in steps:
            arrow(d, [(x1, y), (x2, y)], label)
        note(d, (610, 830, 1100, 920), "Главная идея: reducer не выполняет побочные действия.\nОн только меняет State и выдает Command, которую исполняет Actor.")
    save("03_elm_cycle", (1700, 970), paint)


def add_to_readlist() -> None:
    def paint(d: ImageDraw.ImageDraw) -> None:
        title(d, "Схема 4. Добавление медиапозиции в readlist", 1850)
        boxes = [
            ((40, 260, 300, 370), "Выбор папки\nна карточке каталога"),
            ((390, 260, 650, 370), "CatalogEvent.Ui\nAddToReadingList"),
            ((740, 240, 1000, 390), "CatalogReducer\nпомечает карточку\nloading"),
            ((1090, 260, 1360, 370), "CatalogCommand\nSetReadlistFolder"),
            ((1450, 260, 1680, 370), "CatalogActor"),
            ((1450, 500, 1680, 610), "MediaItemInteractor"),
            ((1090, 500, 1360, 610), "Папка новая?"),
            ((740, 470, 1000, 570), "POST /folders"),
            ((740, 650, 1000, 760), "POST /user-media\nили PATCH folders"),
            ((390, 650, 650, 760), "GET /user-media\nобновление snapshot"),
            ((40, 650, 300, 760), "CatalogEvent.Internal\nItemUpdated"),
            ((40, 840, 300, 930), "CatalogState.items\nкарточка обновлена"),
        ]
        for xy, label in boxes:
            fill, border = (GREEN_FILL, GREEN_BORDER) if "/" in label else (BOX_FILL, BOX_BORDER)
            if "Папка новая" in label:
                fill, border = YELLOW_FILL, YELLOW_BORDER
            box(d, xy, label, fill, border)
        arrow(d, [(300, 315), (390, 315)])
        arrow(d, [(650, 315), (740, 315)])
        arrow(d, [(1000, 315), (1090, 315)])
        arrow(d, [(1360, 315), (1450, 315)])
        arrow(d, [(1565, 370), (1565, 500)])
        arrow(d, [(1450, 555), (1360, 555)])
        arrow(d, [(1090, 535), (1030, 535), (1030, 520), (1000, 520)], "да")
        arrow(d, [(870, 570), (870, 650)])
        arrow(d, [(1090, 580), (1030, 580), (1030, 705), (1000, 705)], "нет")
        arrow(d, [(740, 705), (650, 705)])
        arrow(d, [(390, 705), (300, 705)])
        arrow(d, [(170, 760), (170, 840)])
        note(d, (1090, 780, 1680, 900), "Результат сценария: позиция появляется в readlist,\nа выбранная папка отображается в каталоге и во вкладке «Мои списки».")
    save("04_add_to_readlist", (1850, 1000), paint)


def backend() -> None:
    def paint(d: ImageDraw.ImageDraw) -> None:
        title(d, "Схема 5. Интеграция Android-клиента с backend API", 1800)
        box(d, (700, 110, 1100, 210), "Android app Listly", ACCENT_FILL, ACCENT_BORDER)
        box(d, (290, 320, 620, 440), "AuthRepository\nсессия, token,\nbaseUrl")
        box(d, (1180, 320, 1510, 440), "MediaItemRepository\nкаталог, readlist,\nпапки")
        arrow(d, [(760, 210), (455, 320)])
        arrow(d, [(1040, 210), (1345, 320)])
        auth_eps = [
            ((60, 560, 340, 640), "POST /auth/login"),
            ((380, 560, 660, 640), "POST /auth/register"),
            ((220, 700, 500, 780), "GET /health"),
        ]
        media_eps = [
            ((820, 540, 1100, 620), "GET /media/search"),
            ((1140, 540, 1420, 620), "GET /media/{id}"),
            ((1460, 540, 1740, 620), "GET /user-media"),
            ((820, 680, 1100, 760), "POST /user-media"),
            ((1140, 680, 1420, 760), "PATCH /user-media/{id}/folders"),
            ((1460, 680, 1740, 760), "DELETE /user-media/{id}"),
            ((820, 820, 1100, 900), "GET /folders"),
            ((1140, 820, 1420, 900), "POST /folders"),
            ((1460, 820, 1740, 900), "PATCH/DELETE /folders/{id}"),
        ]
        for xy, label in auth_eps + media_eps:
            box(d, xy, label, GREEN_FILL, GREEN_BORDER, fnt=SMALL_FONT)
        d.line((455, 440, 455, 505), fill=LINE, width=2)
        d.line((200, 505, 520, 505), fill=LINE, width=2)
        for xy, _ in auth_eps:
            cx = (xy[0] + xy[2]) // 2
            arrow(d, [(cx, 505), (cx, xy[1])])

        # Route MediaItemRepository endpoints through a right-side trunk and
        # row buses, so connector lines do not run through endpoint labels.
        d.line((1510, 380, 1780, 380), fill=LINE, width=2)
        d.line((1780, 380, 1780, 795), fill=LINE, width=2)
        for bus_y in (515, 655, 795):
            d.line((960, bus_y, 1780, bus_y), fill=LINE, width=2)
        for xy, _ in media_eps:
            cx = (xy[0] + xy[2]) // 2
            bus_y = 515 if xy[1] == 540 else (655 if xy[1] == 680 else 795)
            arrow(d, [(cx, bus_y), (cx, xy[1])])
        note(d, (620, 925, 1180, 990), "Базовый URL по умолчанию: http://158.160.251.150:8080")
    save("05_backend_integration", (1800, 1040), paint)


def domain_model() -> None:
    def class_box(d: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], name: str, fields: list[str]) -> None:
        x1, y1, x2, y2 = xy
        draw = d
        draw.rounded_rectangle(xy, radius=8, fill=BOX_FILL, outline=BOX_BORDER, width=2)
        draw.rectangle((x1, y1, x2, y1 + 48), fill="#e7ddff", outline=BOX_BORDER, width=2)
        centered_text(draw, (x1, y1, x2, y1 + 48), name, CAPTION_FONT)
        y = y1 + 62
        for field in fields:
            draw.text((x1 + 18, y), field, font=SMALL_FONT, fill=TEXT)
            y += 30

    def paint(d: ImageDraw.ImageDraw) -> None:
        title(d, "Схема 6. Доменная модель Listly", 1600)
        class_box(d, (120, 170, 580, 610), "MediaItem", [
            "id: String",
            "title: String",
            "type: MediaType",
            "inReadlist: Boolean",
            "userMediaId: String?",
            "readlistFolder: ReadlistFolder?",
            "readlistAddedAt: Long?",
            "imageUrl: String?",
            "annotation: String?",
        ])
        class_box(d, (760, 170, 1120, 330), "ReadlistFolder", [
            "title: String",
            "id: String?",
        ])
        class_box(d, (760, 430, 1120, 650), "MediaType", [
            "Movie",
            "Series",
            "Anime",
            "Game",
        ])
        class_box(d, (1180, 430, 1500, 650), "CollectionStatus", [
            "Planned",
            "InProgress",
            "Completed",
            "Dropped",
        ])
        arrow(d, [(580, 300), (760, 250)], "readlistFolder")
        arrow(d, [(580, 430), (760, 540)], "type")
        arrow(d, [(580, 520), (1180, 540)], "status на backend")
        note(d, (1180, 180, 1500, 330), "MediaItem - основная сущность.\nUI может строить поверх нее\nотдельную MediaItemUi.")
    save("06_domain_model", (1600, 730), paint)


def main() -> None:
    architecture()
    navigation()
    elm_cycle()
    add_to_readlist()
    backend()
    domain_model()
    print(f"Generated diagrams in {OUT}")


if __name__ == "__main__":
    main()
