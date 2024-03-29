(ns heraldicon.frontend.main
  (:require
   ["@sentry/browser" :as sentry]
   ["@sentry/integrations" :as sentry-integrations]
   [heraldicon.config :as config]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.core]
   [heraldicon.frontend.dark-mode :as dark-mode]
   [heraldicon.frontend.header :as header]
   [heraldicon.frontend.keys]
   [heraldicon.frontend.language :as language]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.router :as router]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [re-frame.subs :as r-subs]
   [reagent.dom.client :as r]))

(defn app []
  [:<>
   [header/view]
   [:div.main-content.no-scrollbar {:class (dark-mode/class)}
    [router/view]
    [modal/render]
    [auto-complete/render]]])

(defn ^:export init []
  (when (and (not= (config/get :stage) "dev")
             (not (config/get :backend?)))
    (sentry/init
     (clj->js
      {:dsn "https://0723f8737fa50a0ecbae2ade37e83976@o4506989681049600.ingest.us.sentry.io/4506991066021888"
       :environment (config/get :stage)
       :release (str (config/get :commit) "-" (config/get :stage))
       :replaysSessionSampleRate 1.0
       :replaysOnErrorSampleRate 1.0
       :integrations [(sentry-integrations/captureConsoleIntegration (clj->js {:levels "error"}))]})))

  (r-subs/clear-subscription-cache!)
  (rf/dispatch-sync [::state/initialize])
  (rf/dispatch-sync [::language/load-setting])
  (session/read-from-storage)
  (router/start)
  (let [root (r/create-root (.getElementById js/document "app"))]
    (r/render root [app]))
  (let [title-root (r/create-root (first (.getElementsByTagName js/document "title")))]
    (r/render title-root [title/title])))
