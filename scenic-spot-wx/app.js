App({
	globalData: {
		baseUrl: 'http://39.96.52.107:8080',
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
