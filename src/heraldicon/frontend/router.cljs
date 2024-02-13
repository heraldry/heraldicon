(ns heraldicon.frontend.router
  (:require
   [clojure.string :as str]
   [heraldicon.frontend.account :as account]
   [heraldicon.frontend.charge-types :as charge-types]
   [heraldicon.frontend.contact :as contact]
   [heraldicon.frontend.home :as home]
   [heraldicon.frontend.library.arms.details :as library.arms.details]
   [heraldicon.frontend.library.arms.list :as library.arms.list]
   [heraldicon.frontend.library.charge.details :as library.charge.details]
   [heraldicon.frontend.library.charge.list :as library.charge.list]
   [heraldicon.frontend.library.collection.details :as library.collection.details]
   [heraldicon.frontend.library.collection.list :as library.collection.list]
   [heraldicon.frontend.library.ribbon.details :as library.ribbon.details]
   [heraldicon.frontend.library.ribbon.list :as library.ribbon.list]
   [heraldicon.frontend.library.user :as library.user]
   [heraldicon.frontend.maintenance :as maintenance]
   [heraldicon.frontend.news :as news]
   [heraldicon.frontend.status :as status]
   [heraldicon.util.trailing-slash-router :as trailing-slash-router]
   [reagent.core :as rc]
   [reitit.frontend.easy :as reife]))

(derive :route.arms.details/create :route.arms/details)
(derive :route.arms.details/by-id :route.arms/details)
(derive :route.arms.details/by-id-and-version :route.arms/details)
(derive :route.charge.details/create :route.charge/details)
(derive :route.charge.details/by-id :route.charge/details)
(derive :route.charge.details/by-id-and-version :route.charge/details)
(derive :route.collection.details/create :route.collection/details)
(derive :route.collection.details/by-id :route.collection/details)
(derive :route.collection.details/by-id-and-version :route.collection/details)
(derive :route.ribbon.details/create :route.ribbon/details)
(derive :route.ribbon.details/by-id :route.ribbon/details)
(derive :route.ribbon.details/by-id-and-version :route.ribbon/details)

(def ^:private routes
  [["/"
    {:name :route.home/main
     :view home/view}]

   ["/news/"
    {:name :route.news/main
     :view news/view}]

   ["/contact/"
    {:name :route.contact/main
     :view contact/view}]

   ["/collections/"
    {:name :route.collection/list
     :view library.collection.list/view}]

   ["/collections/new"
    {:name :route.collection.details/create
     :view library.collection.details/create-view
     :conflicting true}]

   ["/collections/:id"
    {:name :route.collection.details/by-id
     :view library.collection.details/details-view
     :conflicting true}]

   ["/collections/:id/:version"
    {:name :route.collection.details/by-id-and-version
     :view library.collection.details/details-view}]

   ["/arms/"
    {:name :route.arms/list
     :view library.arms.list/view}]

   ["/arms/new"
    {:name :route.arms.details/create
     :view library.arms.details/create-view
     :conflicting true}]

   ["/arms/:id"
    {:name :route.arms.details/by-id
     :view library.arms.details/details-view
     :conflicting true}]

   ["/arms/:id/:version"
    {:name :route.arms.details/by-id-and-version
     :view library.arms.details/details-view}]

   ["/charges/"
    {:name :route.charge/list
     :view library.charge.list/view}]

   ["/charges/new"
    {:name :route.charge.details/create
     :view library.charge.details/create-view
     :conflicting true}]

   ["/charges/:id"
    {:name :route.charge.details/by-id
     :view library.charge.details/details-view
     :conflicting true}]

   ["/charges/:id/:version"
    {:name :route.charge.details/by-id-and-version
     :view library.charge.details/details-view}]

   ["/ribbons/"
    {:name :route.ribbon/list
     :view library.ribbon.list/view}]

   ["/ribbons/new"
    {:name :route.ribbon.details/create
     :view library.ribbon.details/create-view
     :conflicting true}]

   ["/ribbons/:id"
    {:name :route.ribbon.details/by-id
     :view library.ribbon.details/details-view
     :conflicting true}]

   ["/ribbons/:id/:version"
    {:name :route.ribbon.details/by-id-and-version
     :view library.ribbon.details/details-view}]

   ["/users/"
    {:name :route.user/list
     :view library.user/list-view}]

   ["/users/:username"
    {:name :route.user/details
     :view library.user/details-view}]

   ["/account/"
    {:name :route.account/main
     :view account/view}]

   ["/charge-types/"
    {:name :route.charge-types/main
     :view charge-types/view}]])

(def ^:private router
  (trailing-slash-router/create routes))

(defonce ^:private current-state
  (rc/atom nil))

(defn current-route []
  (-> @current-state :data :name))

(defn- section [route]
  (some-> route namespace (str/split #"[.]") second))

(defn- current-section []
  (section (current-route)))

(defn active-section? [route]
  (= (section route) (current-section)))

(defn- route-view []
  (if-let [page-view (-> @current-state :data :view)]
    [page-view @current-state]
    [status/not-found]))

(defn view []
  (if (maintenance/in-effect? (current-section))
    [maintenance/view]
    [route-view]))

(defn- on-navigate [match _history]
  (trailing-slash-router/fix-path-in-address-bar (:path match))
  (reset! current-state match))

(defn start []
  (reife/start! router on-navigate {:use-fragment false}))
