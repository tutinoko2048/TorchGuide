# Torch Guide

Minecraftで松明を置く間隔を確認できるNeoForge用のクライアントModです。

松明を設置すると、その松明を基準に一定間隔で次の設置候補を表示します。

## 対応環境

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21

## 使い方

1. Modを`mods`フォルダーに入れる
2. ゲーム内で松明を設置する
3. 松明を持った状態で、デフォルトキーの`Y`を押して表示を切り替える

ガイドは、プレイヤーの周囲にある設置可能な場所へ表示されます。表示間隔や範囲はMod設定から変更できます。
設置した松明が20 tick以内に消えると、ガイドの基準は設置前の位置に戻ります。この時間はMod設定で変更できます。

## 設定

- `spacing`: ガイド間の空きブロック数
- `transparency`: ガイドの透明度
- `range`: ガイドを表示する水平方向の範囲
- `undo_window_ticks`: 基準を戻せる時間（10～100 tick、初期値20）

## ビルド

[Stonecutter](https://stonecutter.kikugie.dev/)で複数のMinecraftバージョンを1つのソースからビルドしています。
対応バージョンは`settings.gradle.kts`、バージョンごとの依存関係は`stonecutter.properties.toml`で管理しています。

```text
./gradlew buildAndCollect
```

全バージョンのJarが`build/libs`に出力されます。

## ライセンス

MIT License
