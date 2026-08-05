let wxTimer = null;

function stopWxPoll() {
  if (wxTimer) { clearInterval(wxTimer); wxTimer = null; }
}

function switchLoginTab(tab) {
  document.getElementById('tabPwd').classList.toggle('active', tab === 'pwd');
  document.getElementById('tabWx').classList.toggle('active', tab === 'wx');
  document.getElementById('loginForm').style.display = tab === 'pwd' ? '' : 'none';
  document.getElementById('wxPanel').style.display = tab === 'wx' ? '' : 'none';
  document.getElementById('pwdTip').style.display = tab === 'pwd' ? '' : 'none';
  document.getElementById('wxTip').style.display = tab === 'wx' ? '' : 'none';
  if (tab === 'wx') newWxCode(); else stopWxPoll();
}

async function newWxCode() {
  stopWxPoll();
  const box = document.getElementById('wxCode');
  const qr = document.getElementById('wxQr');
  const hint = document.getElementById('wxHint');
  const status = document.getElementById('wxStatus');
  box.textContent = '------';
  qr.style.display = 'none';
  status.textContent = '正在生成...';
  try {
    const data = await Api.post('/admin/api/wx/login', {});
    if (data.qrBase64) {
      qr.src = 'data:image/png;base64,' + data.qrBase64;
      qr.style.display = 'block';
      box.style.display = 'none';
      hint.innerHTML = '用微信扫一扫上方二维码，在小程序里点「确认登录后台」';
    } else {
      box.style.display = 'block';
      box.textContent = data.code;
      hint.innerHTML = '① 打开微信小程序「个人博客」<br>② 我的 → 管理员 · 微信确认<br>③ 输入上方 6 位验证码，点「确认登录后台」';
    }
    status.textContent = '等待微信确认（10 分钟内有效）';
    wxTimer = setInterval(async () => {
      try {
        const r = await Api.get('/admin/api/wx/login/check?code=' + data.code);
        if (r.status === 'confirmed') {
          stopWxPoll();
          status.textContent = '登录成功，正在进入后台...';
          Api.token = r.token;
          localStorage.setItem('blog_admin_token', r.token);
          location.href = 'index.html';
        } else if (r.status === 'expired') {
          stopWxPoll();
          status.textContent = '验证码已过期，请重新生成';
        }
      } catch (e) { /* 轮询错误忽略 */ }
    }, 2000);
  } catch (e) {
    status.textContent = '生成失败：' + e.message;
  }
}

document.getElementById('loginForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const errorBox = document.getElementById('loginError');
    const btn = document.getElementById('loginBtn');
    errorBox.textContent = '';
    if (!username || !password) {
        errorBox.textContent = '请输入用户名和密码';
        return;
    }
    btn.disabled = true;
    btn.textContent = '登录中...';
    try {
        const data = await Api.post('/admin/api/login', { username, password });
        Api.token = data.token;
        localStorage.setItem('blog_admin_token', data.token);
        location.href = 'index.html';
    } catch (err) {
        errorBox.textContent = err.message;
        btn.disabled = false;
        btn.textContent = '登 录';
    }
});
