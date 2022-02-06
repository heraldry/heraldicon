(ns heraldry.frontend.main
  (:require
   [heraldry.frontend.header :as header]
   [heraldry.frontend.keys] ;; needed for side effects
   [heraldry.frontend.modal :as modal]
   [heraldry.frontend.not-found :as not-found]
   [heraldry.frontend.route :as route]
   [heraldry.frontend.user :as user]
   [heraldry.util :as util]
   [re-frame.core :as rf]
   [reagent.dom :as r]
   [taoensso.timbre :as log]
   [taoensso.tufte :as tufte]))

(defn app []
  [:<>
   [header/view]
   [:div.main-content.no-scrollbar
    (if-let [view (route/view)]
      view
      [not-found/not-found])
    [modal/render]]])

(defn title []
  (util/combine
   " - "
   [@(rf/subscribe [:get-title])
    "Heraldicon"]))

(defonce stats-accumulator (tufte/add-accumulating-handler! {:ns-pattern "*"}))

(defn start-stats-timer [f]
  (rf/dispatch-sync [:set [:ui :timer] (js/setTimeout f 5000)]))

(defn print-stats []
  (log/debug :print-stats)
  (when-let [timer @(rf/subscribe [:get [:ui :timer]])]
    (js/clearTimeout timer))
  (when-let [m (not-empty @stats-accumulator)]
    (log/info "profile\n" (tufte/format-grouped-pstats
                           m
                           {:format-pstats-opts {:columns [:n-calls :min :p50 :p90 :max :mean :clock :total]}})))

  (start-stats-timer print-stats))
(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:heraldry.frontend.language/load-language-setting])
  #_(print-stats)
  (route/start-router)
  (user/load-session-user-data)
  (r/render
   [app]
   (.getElementById js/document "app"))
  (r/render
   [title]
   (first (.getElementsByTagName js/document "title"))))
