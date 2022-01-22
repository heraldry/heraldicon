(ns heraldry.frontend.keys
  (:require
   [heraldry.frontend.arms-library :as arms-library]
   [heraldry.frontend.charge-library :as charge-library]
   [heraldry.frontend.collection-library :as collection-library]
   [heraldry.frontend.ribbon-library :as ribbon-library]
   [heraldry.frontend.route :as route]
   [re-frame.core :as rf]))

(defn key-down-handler [event]
  (let [shift? (.-shiftKey event)
        alt? (.-altKey event)
        ctrl? (.-ctrlKey event)
        meta? (.-metaKey event)
        code (.-code event)
        z-pressed? (= code "KeyZ")
        route-name (-> @route/current-match :data :name)
        undo-path (case route-name
                    :view-arms-by-id arms-library/form-db-path
                    :view-charge-by-id charge-library/form-db-path
                    :view-ribbon-by-id ribbon-library/form-db-path
                    :view-collection-by-id collection-library/form-db-path
                    nil)]
    (cond
      (and undo-path
           (or (and meta? shift? z-pressed?)
               (and ctrl? shift? z-pressed?))) (rf/dispatch [:heraldry.frontend.history.core/redo undo-path])
      (and undo-path
           (or (and meta? z-pressed?)
               (and ctrl? z-pressed?))) (rf/dispatch [:heraldry.frontend.history.core/undo undo-path])
      :else (when (= route-name :view-ribbon-by-id)
              (rf/dispatch [:heraldry.frontend.ribbon-library/edit-set-key-modifiers
                            {:alt? alt?
                             :shift? shift?}])))))

(defn key-up-handler [event]
  (let [shift? (.-shiftKey event)
        alt? (.-altKey event)
        route-name (-> @route/current-match :data :name)]
    (when (= route-name :view-ribbon-by-id)
      (rf/dispatch [:heraldry.frontend.ribbon-library/edit-set-key-modifiers
                    {:alt? alt?
                     :shift? shift?}]))))

(js/window.removeEventListener "keydown" key-down-handler)
(js/window.addEventListener "keydown" key-down-handler)
(js/window.removeEventListener "keyup" key-up-handler)
(js/window.addEventListener "keyup" key-up-handler)
