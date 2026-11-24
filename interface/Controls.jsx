import { Button } from "@heroui/react/button";
import { flushSync } from "react-dom";
import { createRoot } from "react-dom/client";

export function mountViewerBar(dialog) {
  const bar = dialog.querySelector(".dialog-top");
  const focusClose = document.activeElement === bar.querySelector("button");
  const caption = bar.querySelector("#image-caption").textContent;
  const root = createRoot(bar);
  flushSync(() =>
    root.render(
      <>
        <p id="image-caption">{caption}</p>
        <Button
          aria-label="关闭大图"
          onPress={() => dialog.close()}
          render={(props) => (
            <button
              {...props}
              id="dialog-close"
              className={undefined}
              type="button"
            />
          )}
        >
          关闭 <span aria-hidden="true">×</span>
        </Button>
      </>,
    ),
  );
  if (focusClose)
    dialog.querySelector("#dialog-close").focus({ preventScroll: true });
}
