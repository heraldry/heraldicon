(ns heraldicon.frontend.route
  (:require
   [clojure.string :as s]
   [heraldicon.config :as config]
   [heraldicon.frontend.account :as account]
   [heraldicon.frontend.contact :as contact]
   [heraldicon.frontend.home :as home]
   [heraldicon.frontend.library.arms :as library.arms]
   [heraldicon.frontend.library.charge :as library.charge]
   [heraldicon.frontend.library.collection :as library.collection]
   [heraldicon.frontend.library.ribbon :as library.ribbon]
   [heraldicon.frontend.library.user :as library.user]
   [heraldicon.frontend.maintenance :as maintenance]
   [heraldicon.frontend.news :as news]
   [heraldicon.frontend.user :as user]
   [reagent.core :as rc]
   [reitit.frontend :as reif]
   [reitit.frontend.easy :as reife]))

(defonce current-match (rc/atom nil))

(def ^:private routes
  [["/"
    {:name :home
     :view home/view}]

   ["/news/"
    {:name :news
     :view news/view}]

   ["/news"
    {:name :news-without-slash
     :view news/view}]

   ["/contact/"
    {:name :contact
     :view contact/view}]

   ["/contact"
    {:name :contact-without-slash
     :view contact/view}]

   ["/collections/"
    {:name :collections
     :view library.collection/view-list}]

   ["/collections"
    {:name :collections-without-slash
     :view library.collection/view-list}]

   ["/collections/new"
    {:name :create-collection
     :view library.collection/create
     :conflicting true}]

   ["/collections/new/"
    {:name :create-collection-with-slash
     :view library.collection/create
     :conflicting true}]

   ["/collections/:id"
    {:name :view-collection-by-id
     :view library.collection/view-by-id
     :conflicting true}]

   ["/collections/:id/"
    {:name :view-collection-by-id-with-slash
     :view library.collection/view-by-id
     :conflicting true}]

   ["/collections/:id/:version"
    {:name :view-collection-by-id-and-version
     :view library.collection/view-by-id
     :conflicting true}]

   ["/collections/:id/:version/"
    {:name :view-collection-by-id-and-version-with-slash
     :view library.collection/view-by-id
     :conflicting true}]

   ["/arms/"
    {:name :arms
     :view library.arms/view-list}]

   ["/arms"
    {:name :arms-without-slash
     :view library.arms/view-list}]

   ["/arms/new"
    {:name :create-arms
     :view library.arms/create
     :conflicting true}]

   ["/arms/new/"
    {:name :create-arms-with-slash
     :view library.arms/create
     :conflicting true}]

   ["/arms/:id"
    {:name :view-arms-by-id
     :view library.arms/view-by-id
     :conflicting true}]

   ["/arms/:id/"
    {:name :view-arms-by-id-with-slash
     :view library.arms/view-by-id
     :conflicting true}]

   ["/arms/:id/:version"
    {:name :view-arms-by-id-and-version
     :view library.arms/view-by-id
     :conflicting true}]

   ["/arms/:id/:version/"
    {:name :view-arms-by-id-and-version-with-slash
     :view library.arms/view-by-id
     :conflicting true}]

   ["/charges/"
    {:name :charges
     :view library.charge/view-list}]

   ["/charges"
    {:name :charges-without-slash
     :view library.charge/view-list}]

   ["/charges/new"
    {:name :create-charge
     :view library.charge/create
     :conflicting true}]

   ["/charges/new/"
    {:name :create-charge-with-slash
     :view library.charge/create
     :conflicting true}]

   ["/charges/:id"
    {:name :view-charge-by-id
     :view library.charge/view-by-id
     :conflicting true}]

   ["/charges/:id/"
    {:name :view-charge-by-id-with-slash
     :view library.charge/view-by-id
     :conflicting true}]

   ["/charges/:id/:version"
    {:name :view-charge-by-id-and-version
     :view library.charge/view-by-id
     :conflicting true}]

   ["/charges/:id/:version/"
    {:name :view-charge-by-id-and-version-with-slash
     :view library.charge/view-by-id
     :conflicting true}]

   ["/ribbons/"
    {:name :ribbons
     :view library.ribbon/view-list}]

   ["/ribbons"
    {:name :ribbons-without-slash
     :view library.ribbon/view-list}]

   ["/ribbons/new"
    {:name :create-ribbon
     :view library.ribbon/create
     :conflicting true}]

   ["/ribbons/new/"
    {:name :create-ribbon-with-slash
     :view library.ribbon/create
     :conflicting true}]

   ["/ribbons/:id"
    {:name :view-ribbon-by-id
     :view library.ribbon/view-by-id
     :conflicting true}]

   ["/ribbons/:id/"
    {:name :view-ribbon-by-id-with-slash
     :view library.ribbon/view-by-id
     :conflicting true}]

   ["/ribbons/:id/:version"
    {:name :view-ribbon-by-id-and-version
     :view library.ribbon/view-by-id
     :conflicting true}]

   ["/ribbons/:id/:version/"
    {:name :view-ribbon-by-id-and-version-with-slash
     :view library.ribbon/view-by-id
     :conflicting true}]

   ["/users/"
    {:name :users
     :view library.user/view-list
     :conflicting true}]

   ["/users"
    {:name :users-without-slash
     :view library.user/view-list
     :conflicting true}]

   ["/users/:username"
    {:name :view-user
     :view library.user/view-by-username
     :conflicting true}]

   ["/users/:username/"
    {:name :view-user-with-slash
     :view library.user/view-by-username
     :conflicting true}]

   ["/account/"
    {:name :account
     :view account/view}]

   ["/account"
    {:name :account-without-slash
     :view account/view}]])

(def ^:private router
  (reif/router routes))

(defn- resolve-href
  [to path-params query-params]
  (if (keyword? to)
    (reife/href to path-params query-params)
    (let [match (reif/match-by-path router to)
          route (-> match :data :name)
          params (or path-params (:path-params match))
          query (or query-params (:query-params match))]
      (if match
        (reife/href route params query)
        to))))

(defn link
  [{:keys [to path-params query-params class style]} & children]
  (let [href (resolve-href to path-params query-params)]
    (into [:a {:href href
               :class class
               :style style}]
          children)))

(defn- name-matches?
  [name path-params match]
  (and (= name (-> match :data :name))
       (= (not-empty path-params)
          (-> match :parameters :path not-empty))))

(defn- url-matches?
  [to match]
  (let [path (or (:path match) "")
        partial-path (str "/" (name to) "/")]
    (s/starts-with? path partial-path)))

(defn nav-link
  [{:keys [to path-params] :as props} content]
  (let [active (or (name-matches? to path-params @current-match)
                   (url-matches? to @current-match))]
    [:li.nav-menu-item {:class (when active "selected")}
     [link props content]]))

(defn view []
  (when @current-match
    (let [view (:view (:data @current-match))]
      [view @current-match])))

(defn- blocked-by-maintenance-mode? [route-name]
  (-> route-name
      name
      (s/split #"-+")
      first
      #{"arms"
        "collections"
        "ribbons"
        "charges"
        "users"
        "account"
        "create"
        "view"}))

(defn start-router []
  (reife/start!
   router
   (fn [m]
     (when m
       (reset! current-match (cond-> m
                               (and (config/get :maintenance-mode?)
                                    (-> (user/data) :username ((config/get :admins)) not)
                                    (-> m :data :name blocked-by-maintenance-mode?))
                               (assoc-in [:data :view] maintenance/view)))))
   ;; set to false to enable HistoryAPI
   {:use-fragment false}))
