App({
	globalData: {
		baseUrl: 'http://127.0.0.1:8080',
		token: '',
		user: null,
		flashNotice: null
	},
	onLaunch() {
		const token = wx.getStorageSync('token') || '';
		const user = wx.getStorageSync('user') || null;
		this.globalData.token = token;
		this.globalData.user = user;
	}
})
