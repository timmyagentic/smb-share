const dialog = document.querySelector('#image-dialog');
const picture = document.querySelector('#dialog-image');
const caption = document.querySelector('#image-caption');
let opener;
for (const button of document.querySelectorAll('.screenshot-open')) {
  button.addEventListener('click', () => {
    opener = button;
    picture.src = button.dataset.image;
    picture.alt = button.querySelector('img').alt;
    caption.textContent = button.dataset.caption;
    dialog.showModal();
  });
}
document.querySelector('#dialog-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
dialog.addEventListener('close', () => opener?.focus());

const copyInstall = document.querySelector('#copy-install-prompt');
copyInstall?.addEventListener('click', async () => {
  const status = document.querySelector('#copy-install-status');
  try {
    await navigator.clipboard.writeText(document.querySelector('#install-prompt').textContent);
    status.textContent = '已复制，可以粘贴给 Agent。';
  } catch {
    status.textContent = '自动复制不可用，请手动选择上方提示词。';
  }
});
