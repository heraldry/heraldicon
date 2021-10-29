(ns heraldry.frontend.route
  (:require
   [clojure.string :as s]
   [heraldry.frontend.about :as about]
   [heraldry.frontend.account :as account]
   [heraldry.frontend.arms-library :as arms-library]
   [heraldry.frontend.charge-library :as charge-library]
   [heraldry.frontend.collection-library :as collection-library]
   [heraldry.frontend.contact :as contact]
   [heraldry.frontend.home :as home]
   [heraldry.frontend.ribbon-library :as ribbon-library]
   [heraldry.frontend.user-library :as user-library]
   [reagent.core :as rc]
   [reitit.coercion.spec :as rss]
   [reitit.frontend :as reif]
   [reitit.frontend.easy :as reife]))

(defonce current-match (rc/atom nil))

(def routes
  [["/"
    {:name :home
     :view home/view}]

   ["/about/"
    {:name :about
     :view about/view}]

   ["/about"
    {:name :about-without-slash
     :view about/view}]

   ["/contact/"
    {:name :contact
     :view contact/view}]

   ["/contact"
    {:name :contact-without-slash
     :view contact/view}]

   ["/collections/"
    {:name :collections
     :view collection-library/view-list-collection}]

   ["/collections"
    {:name :collections-without-slash
     :view collection-library/view-list-collection}]

   ["/collections/new"
    {:name :create-collection
     :view collection-library/create-collection
     :conflicting true}]

   ["/collections/new/"
    {:name :create-collection-with-slash
     :view collection-library/create-collection
     :conflicting true}]

   ["/collections/:id"
    {:name :view-collection-by-id
     :parameters {:path {:id string?}}
     :view collection-library/view-collection-by-id
     :conflicting true}]

   ["/collections/:id/"
    {:name :view-collection-by-id-with-slash
     :parameters {:path {:id string?}}
     :view collection-library/view-collection-by-id
     :conflicting true}]

   ["/collections/:id/:version"
    {:name :view-collection-by-id-and-version
     :parameters {:path {:id string?
                         :version number?}}
     :view collection-library/view-collection-by-id
     :conflicting true}]

   ["/collections/:id/:version/"
    {:name :view-collection-by-id-and-version-with-slash
     :parameters {:path {:id string?
                         :version number?}}
     :view collection-library/view-collection-by-id
     :conflicting true}]

   ["/arms/"
    {:name :arms
     :view arms-library/view-list-arms}]

   ["/arms"
    {:name :arms-without-slash
     :view arms-library/view-list-arms}]

   ["/arms/new"
    {:name :create-arms
     :view arms-library/create-arms
     :conflicting true}]

   ["/arms/new/"
    {:name :create-arms-with-slash
     :view arms-library/create-arms
     :conflicting true}]

   ["/arms/:id"
    {:name :view-arms-by-id
     :parameters {:path {:id string?}}
     :view arms-library/view-arms-by-id
     :conflicting true}]

   ["/arms/:id/"
    {:name :view-arms-by-id-with-slash
     :parameters {:path {:id string?}}
     :view arms-library/view-arms-by-id
     :conflicting true}]

   ["/arms/:id/:version"
    {:name :view-arms-by-id-and-version
     :parameters {:path {:id string?
                         :version number?}}
     :view arms-library/view-arms-by-id
     :conflicting true}]

   ["/arms/:id/:version/"
    {:name :view-arms-by-id-and-version-with-slash
     :parameters {:path {:id string?
                         :version number?}}
     :view arms-library/view-arms-by-id
     :conflicting true}]

   ["/charges/"
    {:name :charges
     :view charge-library/view-list-charges}]

   ["/charges"
    {:name :charges-without-slash
     :view charge-library/view-list-charges}]

   ["/charges/new"
    {:name :create-charge
     :view charge-library/create-charge
     :conflicting true}]

   ["/charges/new/"
    {:name :create-charge-with-slash
     :view charge-library/create-charge
     :conflicting true}]

   ["/charges/:id"
    {:name :view-charge-by-id
     :parameters {:path {:id string?}}
     :view charge-library/view-charge-by-id
     :conflicting true}]

   ["/charges/:id/"
    {:name :view-charge-by-id-with-slash
     :parameters {:path {:id string?}}
     :view charge-library/view-charge-by-id
     :conflicting true}]

   ["/charges/:id/:version"
    {:name :view-charge-by-id-and-version
     :parameters {:path {:id string?
                         :version number?}}
     :view charge-library/view-charge-by-id
     :conflicting true}]

   ["/charges/:id/:version/"
    {:name :view-charge-by-id-and-version-with-slash
     :parameters {:path {:id string?
                         :version number?}}
     :view charge-library/view-charge-by-id
     :conflicting true}]

   ["/ribbons/"
    {:name :ribbons
     :view ribbon-library/view-list-ribbons}]

   ["/ribbons"
    {:name :ribbons-without-slash
     :view ribbon-library/view-list-ribbons}]

   ["/ribbons/new"
    {:name :create-ribbon
     :view ribbon-library/create-ribbon
     :conflicting true}]

   ["/ribbons/new/"
    {:name :create-ribbon-with-slash
     :view ribbon-library/create-ribbon
     :conflicting true}]

   ["/ribbons/:id"
    {:name :view-ribbon-by-id
     :parameters {:path {:id string?}}
     :view ribbon-library/view-ribbon-by-id
     :conflicting true}]

   ["/ribbons/:id/"
    {:name :view-ribbon-by-id-with-slash
     :parameters {:path {:id string?}}
     :view ribbon-library/view-ribbon-by-id
     :conflicting true}]

   ["/ribbons/:id/:version"
    {:name :view-ribbon-by-id-and-version
     :parameters {:path {:id string?
                         :version number?}}
     :view ribbon-library/view-ribbon-by-id
     :conflicting true}]

   ["/ribbons/:id/:version/"
    {:name :view-ribbon-by-id-and-version-with-slash
     :parameters {:path {:id string?
                         :version number?}}
     :view ribbon-library/view-ribbon-by-id
     :conflicting true}]

   ["/users/"
    {:name :users
     :view user-library/view-list-users
     :conflicting true}]

   ["/users"
    {:name :users-without-slash
     :view user-library/view-list-users
     :conflicting true}]

   ["/users/:username"
    {:name :view-user
     :parameters {:path {:username string?}}
     :view user-library/view-user-by-username
     :conflicting true}]

   ["/users/:username/"
    {:name :view-user-with-slash
     :parameters {:path {:username string?}}
     :view user-library/view-user-by-username
     :conflicting true}]

   ["/account/"
    {:name :account
     :view account/view}]

   ["/account"
    {:name :account-without-slash
     :view account/view}]])

(def router
  (reif/router routes {:data {:coercion rss/coercion}}))

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
    (into
     [:a {:href href
          :class class
          :style style}]
     children)))

(defn- name-matches?
  [name path-params match]
  (and (= name (-> match :data :name))
       (= (not-empty path-params)
          (-> match :parameters :path not-empty))))

(defn- url-matches?
  [url match]
  (= (-> url (s/split #"\?") first)
     (:path match)))

(defn nav-link
  [{:keys [to path-params] :as props} content]
  (let [active (or (name-matches? to path-params @current-match)
                   (url-matches? to @current-match))]
    [:li.nav-menu-item {:class (when active "selected")}
     [link (-> props
               (assoc :active active)) content]]))

(defn view []
  (when @current-match
    (let [view (:view (:data @current-match))]
      [view @current-match])))

(defn start-router []
  (reife/start!
   router
   (fn [m] (reset! current-match m))
   ;; set to false to enable HistoryAPI
   {:use-fragment false}))
