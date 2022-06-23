(ns heraldicon.frontend.debounce
  (:require
   [re-frame.core :as rf]))

(defonce timeouts
  (atom {}))

(defn- clear-timer [id]
  (some-> @timeouts (get id) js/clearTimeout))

(rf/reg-fx ::dispatch
  (fn [[id event-vec n]]
    (clear-timer id)
    (swap! timeouts assoc id
           (js/setTimeout (fn []
                            (rf/dispatch event-vec)
                            (swap! timeouts dissoc id))
                          n))))

(rf/reg-fx ::stop
  (fn [id]
    (clear-timer id)
    (swap! timeouts dissoc id)))
