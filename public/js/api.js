const Api = {
  async call(method, url, body) {
    const opt = { method, headers: {} };
    if (body !== undefined) {
      opt.headers["Content-Type"] = "application/json";
      opt.body = JSON.stringify(body);
    }
    const res = await fetch(url, opt);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || "Có lỗi xảy ra");
    return data;
  },
  get(url) { return this.call("GET", url); },
  post(url, body) { return this.call("POST", url, body); },
  put(url, body) { return this.call("PUT", url, body); },
  del(url) { return this.call("DELETE", url); }
};

const Session = {
  setUser(user) { localStorage.setItem("sc_user", JSON.stringify(user)); },
  getUser() {
    const raw = localStorage.getItem("sc_user");
    return raw ? JSON.parse(raw) : null;
  },
  clear() { localStorage.removeItem("sc_user"); },
  requireLogin() {
    const u = this.getUser();
    if (!u) window.location.href = "/index.html";
    return u;
  }
};

const Cart = {
  get() {
    const raw = localStorage.getItem("sc_cart");
    return raw ? JSON.parse(raw) : [];
  },
  save(items) { localStorage.setItem("sc_cart", JSON.stringify(items)); },
  add(item) {
    const items = this.get();
    const existing = items.find(i => i.itemId === item.itemId && i.note === item.note);
    if (existing) existing.quantity += item.quantity;
    else items.push(item);
    this.save(items);
  },
  clear() { localStorage.removeItem("sc_cart"); },
  total() { return this.get().reduce((sum, i) => sum + i.price * i.quantity, 0); }
};

function money(n) {
  return Number(n).toLocaleString("vi-VN") + " đ";
}

function statusLabel(status) {
  const map = {
    PENDING: "Chờ xác nhận", CONFIRMED: "Đã xác nhận", PREPARING: "Đang chuẩn bị",
    READY: "Sẵn sàng nhận", COMPLETED: "Đã nhận", REJECTED: "Đã từ chối", CANCELLED: "Đã hủy"
  };
  return map[status] || status;
}
