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
   [heraldicon.frontend.search-filter :as search-filter]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [re-frame.subs :as r-subs]
   [reagent.dom.client :as r]
   [taoensso.timbre :as log]))

(defn app []
  [:<>
   [header/view]
   [:div.main-content.no-scrollbar {:class (dark-mode/class)}
    [router/view]
    [modal/render]
    [auto-complete/render]]])

(defonce ^:dynamic *app-root*
  nil)

(defonce ^:dynamic *title-root*
  nil)

(defn app-root []
  (when *app-root*
    (r/unmount *app-root*))
  (set! *app-root* (r/create-root (.getElementById js/document "app")))
  *app-root*)

(defn title-root []
  (when *title-root*
    (r/unmount *title-root*))
  (set! *title-root* (r/create-root (first (.getElementsByTagName js/document "title"))))
  *title-root*)

(defn- get-list-id []
  (let [path js/window.location.pathname]
    ;; this hard-coded matching is not ideal, but we need to know this before the router starts its work;
    ;; maybe someday I'll find a better way, but for now this is good enough
    (case path
      "/arms/" :arms-list
      "/charges/" :charge-list
      "/collections/" :collection-list
      "/ribbons/" :ribbon-list
      nil)))

(defn- restore-search-list-from-url-parameters []
  (when-let [list-id (get-list-id)]
    (rf/dispatch-sync [::search-filter/restore-from-url-parameters list-id])))

(defonce hook-browser-navigation
  (.addEventListener js/window "popstate" restore-search-list-from-url-parameters))

(defn ^:export init []
  (log/info :release (config/get :release))
  (when (and (not= (config/get :stage) "dev")
             (not (config/get :backend?)))
    (sentry/init
     (clj->js
      {:dsn "https://0723f8737fa50a0ecbae2ade37e83976@o4506989681049600.ingest.us.sentry.io/4506991066021888"
       :environment (config/get :stage)
       :release (config/get :release)
       :replaysSessionSampleRate 1.0
       :replaysOnErrorSampleRate 1.0
       :integrations [(sentry-integrations/captureConsoleIntegration (clj->js {:levels "error"}))]})))
  (r-subs/clear-subscription-cache!)
  (rf/dispatch-sync [::state/initialize])
  (rf/dispatch-sync [::language/load-setting])
  (restore-search-list-from-url-parameters)
  (session/read-from-storage)
  (router/start)
  (r/render (app-root) [app])
  (r/render (title-root) [title/title]))
