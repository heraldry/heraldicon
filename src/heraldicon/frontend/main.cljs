(ns heraldicon.frontend.main
  (:require
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.header :as header]
   [heraldicon.frontend.keys]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.router :as router]
   [heraldicon.frontend.user :as user]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]
   [reagent.dom :as r]
   [taoensso.timbre :as log]
   [taoensso.tufte :as tufte]))

(defn app []
  [:<>
   [header/view]
   [:div.main-content.no-scrollbar
    [router/view]
    [modal/render]
    [auto-complete/render]]])

(defn- title []
  (string/combine
   " - "
   [@(rf/subscribe [:get-title])
    "Heraldicon"]))

(defonce ^:private stats-accumulator (tufte/add-accumulating-handler! {:ns-pattern "*"}))

(defn- start-stats-timer [f]
  (rf/dispatch-sync [:set [:ui :timer] (js/setTimeout f 5000)]))

(defn ^:export print-stats []
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
  (router/start)
  (user/load-session-user-data)
  (r/render
   [app]
   (.getElementById js/document "app"))
  (r/render
   [title]
   (first (.getElementsByTagName js/document "title"))))
