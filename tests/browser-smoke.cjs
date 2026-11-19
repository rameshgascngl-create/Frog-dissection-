const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const mime = {'.html': 'text/html', '.jpg': 'image/jpeg', '.avif': 'image/avif', '.js': 'text/javascript'};
const server = http.createServer((req, res) => {
  let file;
  try {
    file = path.resolve(root, '.' + decodeURIComponent(new URL(req.url, 'http://localhost').pathname));
  } catch { res.writeHead(400).end(); return; }
  if (file !== root && !file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  if (file === root) file = path.join(root, 'index.html');
  fs.readFile(file, (err, bytes) => {
    if (err) { res.writeHead(404).end(); return; }
    res.writeHead(200, {'Content-Type': mime[path.extname(file)] || 'application/octet-stream'});
    res.end(bytes);
  });
});

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  const browser = await chromium.launch({headless: true});
  const page = await browser.newPage({viewport: {width: 1366, height: 900}});
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  try {
    await page.goto('http://127.0.0.1:' + port, {waitUntil: 'load'});
    await page.locator('#tab-dissection').waitFor();
    await page.locator('#layer-btn-5').click();
    const photo = page.locator('#photo-specimen');
    async function assertPlate(sex, file) {
      await page.waitForFunction((file) => {
        const img = document.querySelector('#photo-specimen');
        return img?.getAttribute('src') === file && img.complete && img.naturalWidth > 0;
      }, file, {timeout: 15000});
      const actual = await photo.evaluate(el => ({
        src: el.getAttribute('src'), naturalWidth: el.naturalWidth,
        naturalHeight: el.naturalHeight,
        aspect: document.querySelector('#realistic-specimen-container').style.aspectRatio
      }));
      assert.equal(actual.src, file);
      assert.ok(actual.naturalWidth > 0 && actual.naturalHeight > 0, sex + ' failed to decode');
      assert.ok(actual.aspect.includes('/'), sex + ' photograph did not set aspect ratio');
    }
    await assertPlate('male', 'Internal deep male.avif');
    assert.match(await page.locator('#calibration-review').innerText(), /provisional pin positions/i);
    // Calibrate a male marker, then verify that the female position is independent.
    await page.locator('#btn-calibrate-pins').click();
    await page.locator('#calibrate-organ-select').selectOption('gonads');
    const maleBefore = await page.locator('#calibrated-photo-hotspots [data-organ="gonads"] .hotspot-dot').getAttribute('cx');
    await page.locator('#realistic-frog-svg').click({position:{x:170,y:110},force:true});
    assert.match(await page.locator('#calibration-review').innerText(), /locally adjusted pins/i);
    const maleAfter = await page.locator('#calibrated-photo-hotspots [data-organ="gonads"] .hotspot-dot').getAttribute('cx');
    assert.notEqual(maleBefore, maleAfter, 'male calibration did not update the hotspot');
    await page.locator('#btn-calibrate-pins').click();
    await page.locator('#btn-toggle-sex').click();
    assert.match(await page.locator('#calibration-review').innerText(), /provisional pin positions/i);
    const femalePosition = await page.locator('#calibrated-photo-hotspots [data-organ="gonads"] .hotspot-dot').getAttribute('cx');
    assert.notEqual(femalePosition, maleAfter, 'female calibration was overwritten by the male');

    await assertPlate('female', 'Internal deep female.avif');
    await page.locator('#btn-toggle-sex').click();
    await assertPlate('male', 'Internal deep male.avif');
    await page.locator('#layer-btn-3').click();
    await assertPlate('viscera', 'frog viscera.jpg');
    await page.locator('#btn-next-step').click();
    await page.locator('#tab-quiz').click();
    await page.locator('#mcq-question').waitFor();
    assert.ok((await page.locator('#mcq-question').innerText()).length > 15);
    assert.deepEqual(errors, [], 'uncaught application errors');
    console.log('PASS: male/female switch, image decoding, photo alignment, guided step, quiz and no page errors');
  } finally {
    await browser.close();
    await new Promise(resolve => server.close(resolve));
  }
})().catch(e => {console.error(e); process.exitCode = 1;});
