import React, { useState, useRef } from "react";
import {
  Smartphone,
  Monitor,
  Share2,
  ArrowDownToLine,
  QrCode,
  X,
} from "lucide-react";
import { Button } from "@heroui/react/button";
import { Action, Choices, Figure, HeroCopy, Mark, ModalPanel } from "./ui.jsx";
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
  const [screen, setScreen] = useState("share"),
    [qr, setQr] = useState(false);
  const trigger = useRef(null);
  return (
    <section className="pi-hero transfer-hero">
      <div className="pi-container pi-split">
        <HeroCopy
          mark={<Mark Icon={Smartphone}>Android · SMB2/3</Mark>}
          title={
            <>
              把文件，
              <br />
              直接送到 Mac。
            </>
          }
          description="从 Android 分享菜单把照片、视频和文件直接送到局域网里的共享文件夹。"
        >
          <div className="pi-actions">
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
          </div>
          <a className="transfer-movie" href="/assets/share-demo.mp4">
            观看真机传输录像
          </a>
        </HeroCopy>
        <article className="transfer-guide">
          <h2>手机上的三个步骤</h2>
          <Choices
            label="查看传输步骤"
            tabs={screens}
            value={screen}
            onChange={setScreen}
          >
            {(s) => (
              <div className="transfer-panel">
                <div>
                  <h3>{s.title}</h3>
                  <p>{s.body}</p>
                </div>
                <Figure
                  src={s.src}
                  title={s.title}
                  alt={s.body}
                  width={1260}
                  height={2800}
                  priority
                />
              </div>
            )}
          </Choices>
        </article>
      </div>
    </section>
  );
}
