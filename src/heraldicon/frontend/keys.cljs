(ns heraldicon.frontend.keys
  (:require
   [clojure.string :as s]
   [heraldicon.frontend.library.arms :as library.arms]
   [heraldicon.frontend.library.charge :as library.charge]
   [heraldicon.frontend.library.collection :as library.collection]
   [heraldicon.frontend.library.ribbon :as library.ribbon]
   [heraldicon.frontend.route :as route]
   [heraldicon.frontend.state :as state]
   [re-frame.core :as rf]))

(defn entity-edit-page? [entity]
  (let [route-name (-> @route/current-match :data :name name)]
    (or (s/starts-with? route-name (str "view-" entity))
        (s/starts-with? route-name (str "create-" entity)))))

(defn- key-down-handler [event]
  (let [shift? (.-shiftKey event)
        ctrl? (.-ctrlKey event)
        meta? (.-metaKey event)
        code (.-code event)
        z-pressed? (= code "KeyZ")
        undo-path (cond
                    (entity-edit-page? "arms") library.arms/form-db-path
                    (entity-edit-page? "charge") library.charge/form-db-path
                    (entity-edit-page? "ribbon") library.ribbon/form-db-path
                    (entity-edit-page? "collection") library.collection/form-db-path
                    :else nil)]
    (cond
      (and undo-path
           (or (and meta? z-pressed?)
               (and ctrl? z-pressed?))) (if shift?
                                          ;; need to prevent the default here, else this'll
                                          ;; cause dodgy behaviour in text fields
                                          (state/dispatch-on-event-and-prevent-default
                                           event [:heraldicon.frontend.history.core/redo undo-path])
                                          (state/dispatch-on-event-and-prevent-default
                                           event [:heraldicon.frontend.history.core/undo undo-path]))
      (entity-edit-page? "ribbon") (rf/dispatch
                                    [:heraldicon.frontend.library.ribbon/edit-set-key-modifiers
                                     {:shift? shift?}]))))

(defn- key-up-handler [event]
  (let [shift? (.-shiftKey event)]
    (when (entity-edit-page? "ribbon")
      (rf/dispatch
       [:heraldicon.frontend.library.ribbon/edit-set-key-modifiers
        {:shift? shift?}]))))

(js/window.removeEventListener "keydown" key-down-handler)
(js/window.addEventListener "keydown" key-down-handler)
(js/window.removeEventListener "keyup" key-up-handler)
(js/window.addEventListener "keyup" key-up-handler)
