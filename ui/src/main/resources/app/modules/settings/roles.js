define([
  "genesis",
  "modules/status",
  "services/backend",

  "modules/settings/users",
  "modules/validation",

  "backbone",
  "jquery",
  "jvalidate",
  "fcbcomplete"

],

function(genesis, status, backend, Users, validation, Backbone, $) {
  var Roles = genesis.module();

  var LANG = {
    "ROLE_GENESIS_ADMIN": "System Administrator",
    "ROLE_GENESIS_ADMIN.description": "Have full control over genesis application",
    "ROLE_GENESIS_READONLY": "System Read-only",
    "ROLE_GENESIS_READONLY.description": "Have read-only access over all genesis application",
    "ROLE_GENESIS_USER" : "Genesis User",
    "ROLE_GENESIS_USER.description" : "Can login into genesis application",
    "ROLE_GENESIS_PROJECT_ADMIN": "Project Administrator",
    "ROLE_GENESIS_PROJECT_ADMIN.description": "Have full control over project<br/><br/>If environment security enabled, only project admin can create new environments and grant access to them to project users",
    "ROLE_GENESIS_PROJECT_USER": "Project User",
    "ROLE_GENESIS_PROJECT_USER.description": "Have access to project environments<br/> <br/>if environment security disabled, can create and execute actions against all project's environments, <br/>otherwise access is restricted to specific environments"
  };

  Roles.Model = Backbone.Model.extend({
    idAttribute: "name",
    initialize: function(values, options) {
      if(options.projectId) {
        this.projectId = options.projectId;
      }
    },

    parse: function(json) {
      if (json.result) {
        return json.result;
      } else {
        return json;
      }
    },

    url: function() {
      if(this.projectId) {
        return "rest/projects/"+ this.projectId + "/roles/" + this.id;
      } else {
        return "rest/roles/" + this.id
      }
    }
  });

  Roles.Views.Main = Backbone.View.extend({
    events: {
      "click .edit-link": "editRole"
    },

    initialize: function(options) {
      _.bind(this.render, this);
      this.collection = new Backbone.Collection();

      var self = this;
      var rolesLoader = (options.projectId) ? backend.AuthorityManager.projectRoles : backend.AuthorityManager.roles;
      $.when(rolesLoader()).done(function(roles) {
        var modelOptions = (options.projectId) ? { projectId: options.projectId } :  {};
        self.roles = _(roles).map(function(item) {
          return new Roles.Model({name: item}, modelOptions);
        });

        self.listView = new RolesList({collection: self.collection, el: self.el, projectId: options.projectId});
        self.currentView = self.listView;
        self.reloadRoles();
      });

      this.bind("opened", function() {
        if(self.currentView === self.listView ) {
          self.reloadRoles();
        }
      });
    },

    onClose: function() {
      genesis.utils.nullSafeClose(this.currentView);
      genesis.utils.nullSafeClose(this.listView);
    },

    reloadRoles: function(){
      var self = this;
      var allLoad = _(this.roles).map(function(item) { return item.fetch(); });
      $.when.apply($, allLoad).done(function() {
        self.collection.reset(self.roles);
      });
    },

    editRole: function(event) {
      var roleName = $(event.currentTarget).attr("data-role-name"),
          role = this.listView.collection.get(roleName),
          self = this;
      this.currentView = new RoleEdit({role: role, el: this.el});

      this.currentView.bind("back", function() {
        self.currentView.unbind();
        self.currentView.undelegateEvents();
        self.currentView = self.listView;
        self.reloadRoles();
        self.render();
      });
    },

    render: function() {
      if(this.currentView != null) {
        this.currentView.render();
      }
    }
  });

  var RolesList = Backbone.View.extend({
    template: "app/templates/settings/roles/list.html",

    initialize: function(options) {
      this.collection.bind("reset", this.render, this);
      this.projectId = options.projectId;
    },

    render: function() {
      var self = this;
      $.when(genesis.fetchTemplate(this.template)/*, this.collection.fetch()*/).done(function (tmpl) {
        self.$el.html(tmpl({ projectId: self.projectId, roles: self.collection.toJSON(), LANG: LANG }) );
      });
    }
  });

  var RoleEdit = Backbone.View.extend({
    template: "app/templates/settings/roles/edit.html",

    events: {
      "click a.back" : "backToList",
      "click a.save" : "saveChanges"
    },

    initialize: function(options){
      this.role = options.role;
      this.title = options.title || LANG[this.role.get("name")];
      this.showButtons = options.showButtons != undefined ? options.showButtons : true;
      this.render();
    },

    backToList: function() {
      this.trigger("back");
    },

    pullGranties: function() {
      return {
        "users": this.$("#users-select").val() || [],
        "groups":  this.$("#groups-select").val() || []
      };
    },

    saveChanges: function() {
      this.role.set({
        "users": this.$("#users-select").val() || [],
        "groups":  this.$("#groups-select").val() || []
      });
      var self = this;
      this.role.save().done(function () {
        self.backToList();
        status.StatusPanel.success("Changes have been saved");
      })

    },

    initCompletion: function(hasGroups, hasUsers){
      var self = this;

      self.$("#groups-select").fcbkcomplete({
        json_url: hasGroups ? "rest/groups" : null,
        cache: false,
        filter_case: true,
        filter_hide: true,
        filter_selected: true,
        newel: !hasGroups,
        width: "",
        input_name: "groups-select",
        complete_text: "Enter group name...",
        maxitems: 10000
      });

      self.$("#users-select").fcbkcomplete({
        json_url: hasUsers ? "rest/users" : null,
        cache: false,
        filter_case: true,
        filter_hide: true,
        filter_selected: true,
        newel: !hasUsers,
        width: "",
        input_name: "users-select",
        complete_text: "Enter username...",
        maxitems: 10000
      });
    },
    render: function(){

      var self = this;
      $.when(backend.UserManager.hasUsers(), backend.UserManager.hasGroups(), genesis.fetchTemplate(this.template)).done(function(hasUsers, hasGroups, tmpl) {
        self.$el.html(tmpl({
          role: self.role.toJSON(),
          LANG: LANG,
          title: self.title,
          showButtons: self.showButtons
        }));
        self.initCompletion(hasGroups[0], hasUsers[0]);
        self.status = new status.LocalStatus({el: self.$(".notification")});
        validation.bindValidation(self.role, self.$("form"), self.status);
      });
    }
  });

  Roles.Views.Edit = RoleEdit;

  return Roles;
});
