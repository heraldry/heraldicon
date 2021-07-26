(ns heraldry.frontend.main
  (:require [heraldry.frontend.header :as header]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.route :as route]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

(defn app []
  [:<>
   [header/view]
   [:div.main-content.no-scrollbar
    (if-let [view (route/view)]
      view
      [:div "Not found"])
    [modal/render]]])

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (route/start-router)
  (user/load-session-user-data)
  (r/render
   [app]
   (.getElementById js/document "app")))
