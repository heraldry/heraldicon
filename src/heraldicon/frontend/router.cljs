(ns heraldicon.frontend.router
  (:require
   [clojure.string :as s]
   [heraldicon.config :as config]
   [heraldicon.entity.user :as entity.user]
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
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.user :as user]
   [reagent.core :as rc]
   [reitit.core :as r]
   [reitit.frontend :as reif]
   [reitit.frontend.easy :as reife]))

(defonce ^:private current-state
  (rc/atom nil))

(defn current-route []
  (-> @current-state :data :name))

(derive :route.arms.details/by-id :route.arms/details)
(derive :route.arms.details/by-id-and-version :route.arms/details)
(derive :route.charge.details/by-id :route.charge/details)
(derive :route.charge.details/by-id-and-version :route.charge/details)
(derive :route.collection.details/by-id :route.collection/details)
(derive :route.collection.details/by-id-and-version :route.collection/details)
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
     :view library.collection/list-view}]

   ["/collections/new"
    {:name :route.collection/create
     :view library.collection/create-view
     :conflicting true}]

   ["/collections/:id"
    {:name :route.collection.details/by-id
     :view library.collection/details-view
     :conflicting true}]

   ["/collections/:id/:version"
    {:name :route.collection.details/by-id-and-version
     :view library.collection/details-view}]

   ["/arms/"
    {:name :route.arms/list
     :view library.arms/list-view}]

   ["/arms/new"
    {:name :route.arms/create
     :view library.arms/create-view
     :conflicting true}]

   ["/arms/:id"
    {:name :route.arms.details/by-id
     :view library.arms/details-view
     :conflicting true}]

   ["/arms/:id/:version"
    {:name :route.arms.details/by-id-and-version
     :view library.arms/details-view}]

   ["/charges/"
    {:name :route.charge/list
     :view library.charge/list-view}]

   ["/charges/new"
    {:name :route.charge/create
     :view library.charge/create-view
     :conflicting true}]

   ["/charges/:id"
    {:name :route.charge.details/by-id
     :view library.charge/details-view
     :conflicting true}]

   ["/charges/:id/:version"
    {:name :route.charge.details/by-id-and-version
     :view library.charge/details-view}]

   ["/ribbons/"
    {:name :route.ribbon/list
     :view library.ribbon/list-view}]

   ["/ribbons/new"
    {:name :route.ribbon/create
     :view library.ribbon/create-view
     :conflicting true}]

   ["/ribbons/:id"
    {:name :route.ribbon.details/by-id
     :view library.ribbon/details-view
     :conflicting true}]

   ["/ribbons/:id/:version"
    {:name :route.ribbon.details/by-id-and-version
     :view library.ribbon/details-view}]

   ["/users/"
    {:name :route.user/list
     :view library.user/list-view}]

   ["/users/:username"
    {:name :route.user/details
     :view library.user/details-view}]

   ["/account/"
    {:name :route.account/main
     :view account/view}]])

(defn trailing-slash-router [parent]
  ^{:type ::r/router}
  (reify r/Router
    (router-name [_]
      :trailing-slash-handler)
    (routes [_]
      (r/routes parent))
    (compiled-routes [_]
      (r/compiled-routes parent))
    (options [_]
      (r/options parent))
    (route-names [_]
      (r/route-names parent))
    (match-by-path [_ path]
      (or (r/match-by-path parent path)
          (if (s/ends-with? path "/")
            (r/match-by-path parent (subs path 0 (dec (count path))))
            (r/match-by-path parent (str path "/")))))
    (match-by-name [_ name]
      (r/match-by-name parent name))
    (match-by-name [_ name params]
      (r/match-by-name parent name params))))

(def ^:private router
  (trailing-slash-router (reif/router routes)))

(defn- section [route]
  (some-> route namespace (s/split #"[.]") second))

(defn active-section? [route]
  (= (section route) (section (current-state))))

(defn view []
  (if-let [page-view (-> @current-state :data :view)]
    [page-view @current-state]
    [not-found/not-found]))

(defn- blocked-by-maintenance-mode? [route]
  (-> route
      namespace
      (s/split #"[.]")
      second
      #{"arms"
        "collection"
        "ribbon"
        "charge"
        "user"
        "account"}))

(defn- fix-path-in-address-bar [{:keys [path]}]
  (let [real-path (.. js/window -location -pathname)]
    (when-not (= path real-path)
      (some-> js/window.history (.replaceState nil nil path)))))

(defn start []
  (reife/start!
   router
   (fn [match _history]
     (when match
       (fix-path-in-address-bar match)
       (reset! current-state (cond-> match
                               (and (config/get :maintenance-mode?)
                                    (not (entity.user/admin? (user/data)))
                                    (-> match :data :name blocked-by-maintenance-mode?))
                               (assoc-in [:data :view] maintenance/view)))))
   ;; set to false to enable HistoryAPI
   {:use-fragment false}))
