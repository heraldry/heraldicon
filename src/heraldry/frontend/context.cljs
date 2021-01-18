(ns heraldry.frontend.context
  (:require [heraldry.frontend.charge-map :as charge-map]
            [re-frame.core :as rf]))

(def default
  {:load-charge-data       charge-map/fetch-charge-data
   :fn-component-selected? #(do
                              @(rf/subscribe [:ui-component-selected? %]))
   :fn-select-component    #(rf/dispatch [:ui-component-select %])})
