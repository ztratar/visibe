$(function() {
	(function(window) {
		window.VISIBE = window.VISIBE || {Routers: {}, Collections: {}, Models: {}, Views: {}};

		// Settings
		VISIBE.SETTINGS = {
			apiVersion: '0.1'	
		};

		// Utils

		// Models
		VISIBE.Models.MainModel = Backbone.Model.extend();

		VISIBE.Models.Topic = VISIBE.Models.MainModel.extend({
			defaults: {
				title: '',
				statLine: '',
				activityPerInterval: []
			}	
		});

		// Collections
		VISIBE.Collections.MainCollection = Backbone.Collection.extend();

		VISIBE.Collections.Topics = VISIBE.Collections.MainCollection.extend({
			model: VISIBE.Models.Topic,
			urlRoot: VISIBE.SETTINGS.apiVersion + '/topic'
		});

		// Views
		VISIBE.Views.MainView = Backbone.View.extend({
			
		});

		VISIBE.Views.AppView = VISIBE.Views.MainView.extend({
			el: $('body'),
			className: 'visibeApp',

			render: function() {
				this.$pageContainer = this.$('.page-container');
			},

			show: function(view) {
				this.$pageContainer.html(view.$el);
			}
		});

		VISIBE.Views.TopicCardView = VISIBE.Views.MainView.extend({
			tagname: 'li',
			className: 'topic-card-view',

			template: $('#TopicCardView-template').html(),

			render: function() {
				var html = _.template(this.template)({
					model: this.model	
				});
				this.$el.html(html);	
				return this;
			}
		});

		VISIBE.Views.TopicCardsView = VISIBE.Views.MainView.extend({
			tagName: 'ul',
			className: 'topic-cards-view clearfix',

			initialize: function() {
				this.collection.on('reset', this.addAll, this);	
				this.collection.on('add', this.addOne, this);
			},

			addAll: function() {
				this.collection.each(this.addOne);
			},

			addOne: function(model) {
				var topicCardView = new VISIBE.Views.TopicCardView({
					model: model
				});
				this.$el.append(topicCardView.$el);
				topicCardView.render();
			}
		});

		// Routers
		VISIBE.Routers.AppRouter = Backbone.Router.extend({
			routes: {
				'': 'index',
				'topic/:id': 'topic'
			},

			initialize: function() {
				this.appView = new window.VISIBE.Views.AppView();
				this.appView.render();
			},

			index: function() {
				this.topics = this.topics || new window.VISIBE.Collections.Topics();

				this.topicsView = new window.VISIBE.Views.TopicCardsView({
					collection: this.topics	
				});

				this.appView.show(this.topicsView);

				if (!this.topics.length && false) {
					this.topics.fetch({ url: '/topics/hot' });
				} else {
					this.topics.trigger('reset');
					for (var i = 0; i < 9; i++) {
						this.topics.add({
							title: 'test'	
						});
					}
				}
			},

			topic: function(id) {
				this.topic = this.topic || new window.VISIBE.Models.Topic();

				this.topicView = new TopicView({
					model: this.topic	
				});

				this.appView.show(this.topicView);

				if (id !== this.topic.get('id')) {
					this.topic.fetch({ data: { id: id } });
				} else {
					this.topic.trigger('change');
				}

			}
		});
	})(window);

	// Run the app
	window.VISIBE.Runtime = {
		start: function() {
			this.router = new window.VISIBE.Routers.AppRouter();		
			Backbone.history.start({
				pushState: true,
				root: '/visibe/'
			});
		}	
	};

	window.VISIBE.Runtime.start();
});
