(ns heraldicon.frontend.keys
  (:require
   [heraldicon.frontend.library.arms :as library.arms]
   [heraldicon.frontend.library.charge :as library.charge]
   [heraldicon.frontend.library.collection :as library.collection]
   [heraldicon.frontend.library.ribbon :as library.ribbon]
   [heraldicon.frontend.route :as route]
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
        current-route (route/current)
        undo-path (condp (flip isa?) current-route
                    :route.arms/details library.arms/form-db-path
                    :route.charge/details library.charge/form-db-path
                    :route.ribbon/details library.ribbon/form-db-path
                    :route.collection/details library.collection/form-db-path
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
    (when (isa? (route/current) :route.ribbon/details)
      (rf/dispatch
       [:heraldicon.frontend.library.ribbon/edit-set-key-modifiers
        {:shift? shift?}]))))

(js/window.removeEventListener "keydown" key-down-handler)
(js/window.addEventListener "keydown" key-down-handler)
(js/window.removeEventListener "keyup" key-up-handler)
(js/window.addEventListener "keyup" key-up-handler)
