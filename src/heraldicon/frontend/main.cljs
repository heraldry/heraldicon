(ns heraldicon.frontend.main
  (:require
   [heraldicon.frontend.header :as header]
   [heraldicon.frontend.keys] ;; needed for side effects
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.route :as route]
   [heraldicon.frontend.user :as user]
   [heraldicon.util :as util]
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
    [modal/render]
    [auto-complete/render]]])

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
  (rf/dispatch-sync [:heraldicon.frontend.ui.element.blazonry-editor/clear-parser])
  (rf/dispatch-sync [:heraldicon.frontend.language/load-language-setting])
  #_(print-stats)
  (route/start-router)
  (user/load-session-user-data)
  (r/render
   [app]
   (.getElementById js/document "app"))
  (r/render
   [title]
   (first (.getElementsByTagName js/document "title"))))
