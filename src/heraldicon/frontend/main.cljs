(ns heraldicon.frontend.main
  (:require
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
  (r-subs/clear-subscription-cache!)
  (rf/dispatch-sync [::state/initialize])
  (rf/dispatch-sync [::language/load-setting])
  (session/read-from-storage)
  (router/start)
  (let [root (r/create-root (.getElementById js/document "app"))]
    (r/render root [app]))
  (let [title-root (r/create-root (first (.getElementsByTagName js/document "title")))]
    (r/render title-root [title/title])))
