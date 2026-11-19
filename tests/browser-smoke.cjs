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
    // Collect reviewable visual evidence without claiming organ coordinates are approved.
    const auditDir = path.join(root, 'qa-evidence');
    fs.mkdirSync(auditDir, {recursive:true});
    const plateCases = [
      {layer:1, label:'01-external', file:'frog external.jpg'},
      {layer:2, label:'02-muscular', file:'frog muscle.jpg'},
      {layer:3, label:'03-viscera', file:'frog viscera.jpg'},
      {layer:4, label:'04-circulatory', file:'frog heart.jpg'},
      {layer:5, label:'05-urogenital-male', file:'Internal deep male.avif'}
    ];
    const capturedPlates = [];
    for (const p of plateCases) {
      await page.locator('#layer-btn-' + p.layer).click();
      await page.waitForFunction((file) => {
        const img = document.querySelector('#photo-specimen');
        return img?.getAttribute('src') === file && img.complete && img.naturalWidth > 0;
      }, p.file, {timeout:15000});
      const details = await page.locator('#photo-specimen').evaluate(img => ({
        width:img.naturalWidth,height:img.naturalHeight,
        src:img.getAttribute('src'),
        hotspots:document.querySelectorAll('#calibrated-photo-hotspots .organ-hotspot').length
      }));
      capturedPlates.push({view:p.label, ...details, reviewStatus:'PROVISIONAL'});
      if (p.layer === 1) {
        assert.equal(await page.locator('#calibrated-photo-hotspots [data-organ="cloaca"]').count(), 0,
          'side-view external photograph must not claim an exposed cloacal opening');
        assert.equal(await page.locator('#calibrated-photo-hotspots [data-organ="vocal_sacs"]').count(), 0,
          'unexposed vocal sac must not have a photographic hotspot');
      }
      if (p.layer === 2) {
        assert.equal(await page.locator('#calibrated-photo-hotspots [data-organ="hindlimb_muscles"]').count(), 0,
          'cropped muscle teaching plate must not suggest fully exposed deep hindlimb muscles');
      }
      if (p.layer === 4) {
        assert.equal(await page.locator('#calibrated-photo-hotspots [data-organ="venous_system"]').count(), 0,
          'heart close-up does not demonstrate the portal venous system');
        assert.equal(await page.locator('#calibrated-photo-hotspots [data-organ="ventricle"]').count(), 1,
          'ventricle must remain available');
      }
      await page.locator('#realistic-specimen-container').screenshot({
        path:path.join(auditDir,p.label+'.png')
      });
    }
    await page.locator('#btn-toggle-sex').click();
    await page.waitForFunction(() => {
      const img = document.querySelector('#photo-specimen');
      return img?.getAttribute('src') === 'Internal deep female.avif' && img.complete && img.naturalWidth > 0;
    });
    const femaleDetails = await page.locator('#photo-specimen').evaluate(img => ({
      width:img.naturalWidth,height:img.naturalHeight,
      src:img.getAttribute('src'),
      hotspots:document.querySelectorAll('#calibrated-photo-hotspots .organ-hotspot').length
    }));
    capturedPlates.push({view:'06-urogenital-female',...femaleDetails,reviewStatus:'PROVISIONAL'});
    await page.locator('#realistic-specimen-container').screenshot({
      path:path.join(auditDir,'06-urogenital-female.png')
    });
    fs.writeFileSync(path.join(auditDir,'plate-index.json'), JSON.stringify(capturedPlates,null,2));
    assert.equal(capturedPlates.length,6);
    await page.locator('#btn-toggle-sex').click();
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
    // The exported manifest must carry independent coordinates for both sexes.
    const downloadPromise = page.waitForEvent('download');
    await page.locator('#btn-export-calibration').click();
    const calibrationDownload = await downloadPromise;
    assert.equal(calibrationDownload.suggestedFilename(), 'frog-calibration-review.json');
    const stream = await calibrationDownload.createReadStream();
    let serialized = '';
    for await (const chunk of stream) serialized += chunk.toString('utf8');
    const review = JSON.parse(serialized);
    assert.equal(review.schema, 'frog-dissection-calibration-v1');
    assert.equal(review.images['5_male'], 'Internal deep male.avif');
    assert.equal(review.images['5_female'], 'Internal deep female.avif');
    assert.equal(review.coordinates['5_male'].gonads.x, Number(maleAfter));
    assert.notEqual(review.coordinates['5_male'].gonads.x, review.coordinates['5_female'].gonads.x);
    await page.locator('#btn-calibrate-pins').click();
    await page.locator('#btn-toggle-sex').click();
    assert.match(await page.locator('#calibration-review').innerText(), /provisional pin positions/i);
    const femalePosition = await page.locator('#calibrated-photo-hotspots [data-organ="gonads"] .hotspot-dot').getAttribute('cx');
    assert.notEqual(femalePosition, maleAfter, 'female calibration was overwritten by the male');

    await assertPlate('female', 'Internal deep female.avif');
    assert.equal(await page.locator('#layer-organ-chips [data-key="oviducts"]').count(), 1);
    assert.equal(await page.locator('#calibrated-photo-hotspots [data-organ="oviducts"]').count(), 0,
      'unreviewed oviduct should not have an invented position');
    await page.locator('#btn-calibrate-pins').click();
    await page.locator('#calibrate-organ-select').selectOption('oviducts');
    await page.locator('#realistic-frog-svg').click({position:{x:200,y:90},force:true});
    assert.equal(await page.locator('#calibrated-photo-hotspots [data-organ="oviducts"]').count(), 1,
      'oviduct becomes available after manual calibration');
    await page.locator('#btn-calibrate-pins').click();
    await page.locator('#btn-toggle-sex').click();
    assert.equal(await page.locator('#layer-organ-chips [data-key="oviducts"]').count(), 0,
      'male view must not show female oviducts');
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
