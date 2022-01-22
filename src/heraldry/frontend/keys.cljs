(ns heraldry.frontend.keys
  (:require
   [clojure.string :as s]
   [heraldry.frontend.arms-library :as arms-library]
   [heraldry.frontend.charge-library :as charge-library]
   [heraldry.frontend.collection-library :as collection-library]
   [heraldry.frontend.ribbon-library :as ribbon-library]
   [heraldry.frontend.route :as route]
   [heraldry.frontend.state :as state]
   [re-frame.core :as rf]))

(defn entity-edit-page? [entity]
  (let [route-name (-> @route/current-match :data :name name)]
    (or (-> route-name (s/starts-with? (str "view-" entity)))
        (-> route-name (s/starts-with? (str "create-" entity))))))

(defn key-down-handler [event]
  (let [shift? (.-shiftKey event)
        alt? (.-altKey event)
        ctrl? (.-ctrlKey event)
        meta? (.-metaKey event)
        code (.-code event)
        z-pressed? (= code "KeyZ")
        undo-path (cond
                    (entity-edit-page? "arms") arms-library/form-db-path
                    (entity-edit-page? "charge") charge-library/form-db-path
                    (entity-edit-page? "ribbon") ribbon-library/form-db-path
                    (entity-edit-page? "collection") collection-library/form-db-path
                    :else nil)]
    (cond
      (and undo-path
           (or (and meta? z-pressed?)
               (and ctrl? z-pressed?))) (if shift?
                                          ;; need to prevent the default here, else this'll
                                          ;; cause dodgy behaviour in text fields
                                          (state/dispatch-on-event-and-prevent-default
                                           event [:heraldry.frontend.history.core/redo undo-path])
                                          (state/dispatch-on-event-and-prevent-default
                                           event [:heraldry.frontend.history.core/undo undo-path]))
      (entity-edit-page? "ribbon") (rf/dispatch
                                    [:heraldry.frontend.ribbon-library/edit-set-key-modifiers
                                     {:alt? alt?
                                      :shift? shift?}]))))

(defn key-up-handler [event]
  (let [shift? (.-shiftKey event)
        alt? (.-altKey event)]
    (when (entity-edit-page? "ribbon")
      (rf/dispatch
       [:heraldry.frontend.ribbon-library/edit-set-key-modifiers
        {:alt? alt?
         :shift? shift?}]))))

(js/window.removeEventListener "keydown" key-down-handler)
(js/window.addEventListener "keydown" key-down-handler)
(js/window.removeEventListener "keyup" key-up-handler)
(js/window.addEventListener "keyup" key-up-handler)
