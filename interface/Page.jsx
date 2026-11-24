import React, { useState, useRef } from "react";
import {
  Smartphone,
  Monitor,
  Share2,
  ArrowDownToLine,
  QrCode,
} from "lucide-react";
import { Button } from "@heroui/react/button";
import { Action, Choices, Figure, ModalPanel } from "./ui.jsx";
export const site = {
  name: "SMB Share",
  lang: "zh",
  nav: [
    ["设置步骤", "#how"],
    ["手机界面", "#screenshots"],
    ["传输性能", "#performance"],
    ["常见问题", "#faq"],
  ],
};
const screens = [
  {
    id: "share",
    label: "选择分享",
    icon: Share2,
    src: "/assets/share-sheet.png",
    title: "从系统分享菜单发送",
    body: "在相册或文件管理器中选好文件，点分享，再选 SMB 分享。",
  },
  {
    id: "setup",
    label: "连接 Mac",
    icon: Monitor,
    src: "/assets/app-home.png",
    title: "连接你的共享文件夹",
    body: "第一次使用时填写 Mac 或 SMB2/3 共享的地址和凭证。",
  },
  {
    id: "transfer",
    label: "查看进度",
    icon: Smartphone,
    src: "/assets/transfer-progress.png",
    title: "确认文件传输进度",
    body: "传输大文件时保持应用在前台，完成后再离开。",
  },
];
export function Hero() {
  const [screen, setScreen] = useState("share");
  const [qr, setQr] = useState(false);
  const trigger = useRef(null);
  return (
    <section className="pi-hero transfer-hero" aria-labelledby="hero-title">
      <div className="pi-container">
        <header className="transfer-heading">
          <div>
            <h1 id="hero-title">SMB Share</h1>
            <p>在 Android 的分享菜单里，把文件发到 Mac 或局域网共享文件夹。</p>
          </div>
          <div className="transfer-actions">
            <Action
              href="/downloads/SMB-Share-1.1.1.apk"
              icon={ArrowDownToLine}
            >
              下载 Android APK
            </Action>
            <Button
              ref={trigger}
              className="pi-action pi-ghost"
              aria-haspopup="dialog"
              onPress={() => setQr(true)}
            >
              <QrCode size={18} />
              手机扫码
            </Button>
          </div>
        </header>
        <ModalPanel
          open={qr}
          onChange={setQr}
          title="用手机打开官网"
          closeLabel="关闭二维码"
          className="transfer-qr"
          returnFocus={trigger}
        >
          <img
            src="/assets/download-qr.svg"
            width={224}
            height={224}
            alt="SMB Share 官方下载页二维码"
          />
          <a href="/downloads/SMB-Share-1.1.1.apk">下载 APK</a>
        </ModalPanel>
        <div className="transfer-stage">
          <div className="transfer-step-reading">
            <h2>从手机发送文件</h2>
            <Choices
              label="查看传输步骤"
              tabs={screens}
              value={screen}
              onChange={setScreen}
            >
              {(item) => (
                <div className="transfer-instruction">
                  <h3>{item.title}</h3>
                  <p>{item.body}</p>
                  <a href="/assets/share-demo.mp4">观看真机录像</a>
                </div>
              )}
            </Choices>
          </div>
          <div className="transfer-contact-sheet" aria-label="三张真机截图">
            {screens.map((s) => (
              <figure
                key={s.id}
                data-current={s.id === screen ? "true" : "false"}
              >
                <Figure
                  src={s.src}
                  title={s.title}
                  alt={s.body}
                  width={1260}
                  height={2800}
                  priority={s.id === "share"}
                />
                <figcaption>{s.label}</figcaption>
              </figure>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
