(ns heraldry.frontend.main
  (:require [heraldry.frontend.header :as header]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.route :as route]
            [heraldry.frontend.user :as user]
            [heraldry.util :as util]
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

(defn title []
  (util/combine
   " - "
   [@(rf/subscribe [:get-title])
    "Heraldry Digital"]))

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:heraldry.frontend.language/load-language-setting])
  (route/start-router)
  (user/load-session-user-data)
  (r/render
   [app]
   (.getElementById js/document "app"))
  (r/render
   [title]
   (first (.getElementsByTagName js/document "title"))))
