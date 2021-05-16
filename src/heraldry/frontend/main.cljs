(ns heraldry.frontend.main
  (:require [devtools.core :as devtools]
            [heraldry.frontend.header :as header]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.route :as route]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

(defn app []
  [:<>
   [header/view]
   [:div.main-content
    (if-let [view (route/view)]
      view
      [:div "Not found"])
    [modal/render]]])

(defn stop [])

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (route/start-router)
  (user/load-session-user-data)
  (r/render
   [app]
   (.getElementById js/document "app")))

(defn ^:export init []
  (start))

(defn setup-devtools []
  (let [{:keys [cljs-land-style]} (devtools/get-prefs)]
    (devtools/set-pref! :cljs-land-style (str "filter:invert(1);" cljs-land-style)))
  (devtools/install!))

(defn dev-init []
  (setup-devtools)
  (init))
