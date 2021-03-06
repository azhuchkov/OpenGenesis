define([
  "genesis",
  "modules/status",
  "backbone",
  "jquery",
  "jvalidate"

],

function(genesis, status, Backbone, $) {

  var Plugins = genesis.module();

  Plugins.Model = Backbone.Model.extend({});

  Plugins.Collection = Backbone.Collection.extend({
    model: Plugins.Model,
    url: "rest/plugins"
  });

  Plugins.Views.Main = Backbone.View.extend({
    template: "app/templates/settings/plugins.html",

    events: {
      "click .edit-settings": "editSettings"
    },

    initialize: function(options) {
      _.bind(this.render, this);
      this.collection = new Plugins.Collection();

      var self = this;
      this.collection.fetch().done(function() {
        self.listView = new PluginsList({collection: self.collection, el: self.el});
        self.currentView = self.listView;
        self.render();
      });
      this.mainView = options.main;
    },

    onClose: function() {
      genesis.utils.nullSafeClose(this.currentView);
      genesis.utils.nullSafeClose(this.listView);
    },

    editSettings: function(element) {
      var id = $(element.currentTarget).attr("data-plugin-id");
      var plugin = this.collection.get(id);
      this.currentView = new PluginSettings({model: plugin, el: this.el});

      var self = this;
      this.currentView.bind("back", function() {
        self.currentView.unbind();
        self.currentView.undelegateEvents();
        self.currentView = self.listView;
        self.render();
      });
    },

    render: function() {
      if(this.currentView != null) {
        this.currentView.render();
        this.mainView.toggleRestart();
      }
    }
  });

  var PluginsList = Backbone.View.extend({
    template: "app/templates/settings/plugins.html",

    render: function() {
      var view = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl){
        view.$el.html(tmpl({plugins: view.collection.toJSON()}));
      });
    }
  });

  var PluginSettings = Backbone.View.extend({
    template: "app/templates/settings/plugin_settings.html",

    events: {
      "click a.back" : "backToPlugins",
      "click a.save" : "savePluginSettings"
    },

    initialize: function(options) {
      this.plugin = this.model;
      var self = this;
      this.plugin.fetch().done(function(){
        self.configMap = _(self.plugin.get("configuration")).reduce(
          function (memo, config) { memo[config.name] = config.value; return memo; }, {}
        );
        self.render();
      });
    },

    backToPlugins: function() {
      this.trigger("back");
    },

    savePluginSettings: function (){
      var configuration = _(this.plugin.get('configuration')).map(_.clone);
      _(configuration).each(function (item) {
        if (!item.readOnly) {
          item.value = $("input[name='" + item.name + "']").val()
        }
      });

      this.plugin.set('configuration', configuration, {"silent": true});

      if (this.plugin.hasChanged('configuration')) {
        var self = this;
        this.plugin.save()
          .done(function () {
            status.StatusPanel.success("Plugin settings updated");
            self.trigger("back");
          })
          .error(function () {
            status.StatusPanel.error("Failed to process request");
          });
      } else {
        this.trigger("back");
      }
    },

    render: function(){
      var view = this;
      $.when(genesis.fetchTemplate(this.template)).done(function(tmpl) {
        var settings = _(view.plugin.get("configuration")).groupBy(function(item) { return item.readOnly ? "constants" : "properties" } );
        view.$el.html(tmpl({plugin: view.plugin.toJSON(), settings: settings}));
      });
    }
  });

  return Plugins;
});
