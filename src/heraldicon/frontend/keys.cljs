(ns heraldicon.frontend.keys
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.library.arms.shared :as library.arms.shared]
   [heraldicon.frontend.library.charge.shared :as library.charge.shared]
   [heraldicon.frontend.library.collection.shared :as library.collection.shared]
   [heraldicon.frontend.library.ribbon.shared :as library.ribbon.shared]
   [heraldicon.frontend.router :as router]
   [heraldicon.frontend.state :as state]
   [re-frame.core :as rf]))

(defn- flip [f]
  (fn [a b]
    (f b a)))

(defn- key-down-handler [event]
  (let [shift? (.-shiftKey event)
        ctrl? (.-ctrlKey event)
        meta? (.-metaKey event)
        code (.-code event)
        z-pressed? (= code "KeyZ")
        current-route (router/current-route)
        undo-path (condp (flip isa?) current-route
                    :route.arms/details (form/data-path library.arms.shared/form-id)
                    :route.charge/details (form/data-path library.charge.shared/form-id)
                    :route.ribbon/details library.ribbon.shared/form-db-path
                    :route.collection/details library.collection.shared/form-db-path
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
      (isa? current-route :route.ribbon/details) (rf/dispatch
                                                  [:heraldicon.frontend.library.ribbon/edit-set-key-modifiers
                                                   {:shift? shift?}]))))

(defn- key-up-handler [event]
  (let [shift? (.-shiftKey event)]
    (when (isa? (router/current-route) :route.ribbon/details)
      (rf/dispatch
       [:heraldicon.frontend.library.ribbon/edit-set-key-modifiers
        {:shift? shift?}]))))

(js/window.removeEventListener "keydown" key-down-handler)
(js/window.addEventListener "keydown" key-down-handler)
(js/window.removeEventListener "keyup" key-up-handler)
(js/window.addEventListener "keyup" key-up-handler)
