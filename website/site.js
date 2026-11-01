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

const demoVideo = document.getElementById('demo-video');
const playback = document.querySelector('[data-demo-playback]');
if (demoVideo && playback) {
  const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const updateLabel = () => {
    playback.textContent = demoVideo.paused ? '播放演示' : '暂停演示';
  };
  const play = () => demoVideo.play().catch(updateLabel);
  demoVideo.addEventListener('play', updateLabel);
  demoVideo.addEventListener('pause', updateLabel);
  playback.addEventListener('click', () => {
    if (demoVideo.paused) play();
    else demoVideo.pause();
  });
  demoVideo.controls = false;
  playback.hidden = false;
  if (!motion.matches) play();
  motion.addEventListener('change', event => {
    if (event.matches) demoVideo.pause();
  });
}
