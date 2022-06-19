(ns heraldicon.frontend.title
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(def ^:private title-path [:ui :title])

(macros/reg-event-db ::set
  (fn [db [_ value]]
    (assoc-in db title-path value)))

(macros/reg-event-db ::set-from-path-or-default
  (fn [db [_ path default]]
    (let [current (get-in db title-path)
          new-title (or (get-in db path) default)]
      (if (not= new-title current)
        (assoc-in db title-path new-title)
        db))))

(defn title []
  [tr (string/str-tr @(rf/subscribe [:get title-path]) " - Heraldicon")])
