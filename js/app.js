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

				$(document).on('click', 'a', function(ev) {
					var currentTarget = $(ev.currentTarget),
						href = currentTarget.attr('href');

					ev.preventDefault();

					if (href.indexOf('/') === 0) {
						window.VISIBE.Runtime.router.navigate(href, true);
						return false;	
					}
				});
			},

			show: function(view) {
				this.$pageContainer.html(view.$el);
			},

			expandHeader: function() {
				this.$el.removeClass('contract-header');
				$('.topic-cards-view').removeClass('card-opened');
			},

			contractHeader: function() {
				this.$el.addClass('contract-header');
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

			events: {
				'click .topic-card-view a': 'cardOpened'
			},

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
			},

			cardOpened: function(ev) {
				window.VISIBE.Runtime.router.navigate('/topic');
				$(ev.currentTarget).parent().addClass('active');
				this.$el.addClass('card-opened');	
				window.VISIBE.Runtime.router.appView.contractHeader();
			}
		});

		VISIBE.Views.TopicView = VISIBE.Views.MainView.extend({
			className: 'topic-view',

			template: $('#TopicView-template').html(),

			render: function() {
				var html = _.template(this.template)({
					model: this.model	
				});
				this.$el.html(html);	
				return this;
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

				this.topicsView = this.topicsView || new window.VISIBE.Views.TopicCardsView({
					collection: this.topics	
				});

				this.appView.show(this.topicsView);
				this.appView.expandHeader();

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

				this.topicView = this.topicView || new window.VISIBE.Views.TopicView({
					model: this.topic	
				});

				this.appView.show(this.topicView);
				this.appView.contractHeader();

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
