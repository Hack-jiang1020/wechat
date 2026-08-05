const Api = {
    token: localStorage.getItem('blog_admin_token') || '',

    async request(method, url, data) {
        const options = {
            method,
            headers: { 'Content-Type': 'application/json', token: this.token }
        };
        if (data !== undefined) {
            options.body = JSON.stringify(data);
        }
        const res = await fetch(url, options);
        let json;
        try {
            json = await res.json();
        } catch (e) {
            throw new Error('服务响应异常，请检查后端是否启动');
        }
        if (json.code === 401) {
            localStorage.removeItem('blog_admin_token');
            location.href = 'login.html';
            throw new Error(json.msg);
        }
        if (json.code !== 200) {
            throw new Error(json.msg || '请求失败');
        }
        return json.data;
    },

    get(url) {
        return this.request('GET', url);
    },

    post(url, data) {
        return this.request('POST', url, data);
    },

    del(url) {
        return this.request('DELETE', url);
    },

    async upload(file) {
        const fd = new FormData();
        fd.append('file', file);
        const res = await fetch('/admin/api/upload', {
            method: 'POST',
            headers: { token: this.token },
            body: fd
        });
        const json = await res.json();
        if (json.code !== 200) {
            throw new Error(json.msg || '上传失败');
        }
        return json.data;
    }
};
