(ns heraldry.frontend.ui.element.charge-group-preset-select
  (:require [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.shared :as shared]
            [re-frame.core :as rf]))

(def presets
  [["Ordinaries"
    ["Fesswise" {:type :heraldry.charge-group.type/rows
                 :strip-angle 0
                 :spacing 25
                 :stretch 0.866
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}}]
                 :strips [{:slots [0 0 0]}]}]
    ["Palewise" {:type :heraldry.charge-group.type/columns
                 :strip-angle 0
                 :spacing 20
                 :stretch 0.866
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}}]
                 :strips [{:slots [0 0 0]}]}]
    ["Bendwise" {:type :heraldry.charge-group.type/rows
                 :strip-angle 45
                 :spacing 25
                 :stretch 0.866
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}}]
                 :strips [{:slots [0 0 0]}]}]
    ["Bendwise (sinister)" {:type :heraldry.charge-group.type/rows
                            :strip-angle -45
                            :spacing 25
                            :stretch 0.866
                            :charges [{:type :heraldry.charge.type/roundel
                                       :field {:type :heraldry.field.type/plain
                                               :tincture :azure}}]
                            :strips [{:slots [0 0 0]}]}]
    ["Chevronwise" {:type :heraldry.charge-group.type/rows
                    :strip-angle 0
                    :spacing 30
                    :stretch 0.6
                    :charges [{:type :heraldry.charge.type/roundel
                               :field {:type :heraldry.field.type/plain
                                       :tincture :azure}}]
                    :strips [{:slots [0]}
                             {:slots [0 0]}
                             {:slots [0 nil 0]}]}]
    ["In cross" {:type :heraldry.charge-group.type/rows
                 :strip-angle 0
                 :spacing 20
                 :stretch 1
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}}]
                 :strips [{:slots [nil nil 0 nil nil]}
                          {:slots [nil nil 0 nil nil]}
                          {:slots [0 0 0 0 0]}
                          {:slots [nil nil 0 nil nil]}
                          {:slots [nil nil 0 nil nil]}]}]
    ["In saltire" {:type :heraldry.charge-group.type/rows
                   :strip-angle 45
                   :spacing 20
                   :stretch 1
                   :charges [{:type :heraldry.charge.type/roundel
                              :field {:type :heraldry.field.type/plain
                                      :tincture :azure}}]
                   :strips [{:slots [nil nil 0 nil nil]}
                            {:slots [nil nil 0 nil nil]}
                            {:slots [0 0 0 0 0]}
                            {:slots [nil nil 0 nil nil]}
                            {:slots [nil nil 0 nil nil]}]}]
    ["In pall" {:type :heraldry.charge-group.type/columns
                :strip-angle 0
                :spacing 14
                :stretch 0.866
                :charges [{:type :heraldry.charge.type/roundel
                           :field {:type :heraldry.field.type/plain
                                   :tincture :azure}
                           :geometry {:size 18}}]
                :strips [{:slots [0 nil nil nil nil]}
                         {:slots [nil 0 nil nil nil]}
                         {:slots [nil nil 0 0 0]
                          :stretch 1.25}
                         {:slots [nil 0 nil nil nil]}
                         {:slots [0 nil nil nil nil]}]}]
    ["Three in above bend" {:type :heraldry.charge-group.type/rows
                            :strip-angle 0
                            :spacing 30
                            :stretch 1
                            :charges [{:type :heraldry.charge.type/roundel
                                       :field {:type :heraldry.field.type/plain
                                               :tincture :azure}
                                       :geometry {:size 20}}]
                            :strips [{:slots [0 0]}
                                     {:slots [nil 0]}]}]
    ["Three below bend" {:type :heraldry.charge-group.type/rows
                         :strip-angle 45
                         :spacing 50
                         :stretch 0.3
                         :charges [{:type :heraldry.charge.type/roundel
                                    :field {:type :heraldry.field.type/plain
                                            :tincture :azure}
                                    :geometry {:size 20}}]
                         :strips [{:slots [0 0]}
                                  {:slots [0]}]}]]
   ["Triangular"
    ["Three" {:type :heraldry.charge-group.type/rows
              :strip-angle 0
              :spacing 40
              :stretch 0.866
              :charges [{:type :heraldry.charge.type/roundel
                         :field {:type :heraldry.field.type/plain
                                 :tincture :azure}}]
              :strips [{:slots [0 0]}
                       {:slots [0]}]}]
    ["Three (inverted)" {:type :heraldry.charge-group.type/rows
                         :strip-angle 0
                         :spacing 40
                         :stretch 0.866
                         :charges [{:type :heraldry.charge.type/roundel
                                    :field {:type :heraldry.field.type/plain
                                            :tincture :azure}}]
                         :strips [{:slots [0]}
                                  {:slots [0 0]}]}]

    ["Three (columns)" {:type :heraldry.charge-group.type/columns
                        :strip-angle 0
                        :spacing 30
                        :stretch 0.866
                        :charges [{:type :heraldry.charge.type/roundel
                                   :field {:type :heraldry.field.type/plain
                                           :tincture :azure}}]
                        :strips [{:slots [0 0]}
                                 {:slots [0]}]}]
    ["Three (columns, inverted)" {:type :heraldry.charge-group.type/columns
                                  :strip-angle 0
                                  :spacing 30
                                  :stretch 0.866
                                  :charges [{:type :heraldry.charge.type/roundel
                                             :field {:type :heraldry.field.type/plain
                                                     :tincture :azure}}]
                                  :strips [{:slots [0]}
                                           {:slots [0 0]}]}]
    ["Six" {:type :heraldry.charge-group.type/rows
            :strip-angle 0
            :spacing 30
            :stretch 0.866
            :charges [{:type :heraldry.charge.type/roundel
                       :field {:type :heraldry.field.type/plain
                               :tincture :azure}}]
            :strips [{:slots [0 0 0]}
                     {:slots [0 0]}
                     {:slots [0]}]}]]
   ["Grid"
    ["Square" {:type :heraldry.charge-group.type/rows
               :strip-angle 0
               :spacing 25
               :stretch 1
               :charges [{:type :heraldry.charge.type/roundel
                          :field {:type :heraldry.field.type/plain
                                  :tincture :azure}}]
               :strips [{:slots [0 0 0]}
                        {:slots [0 0 0]}
                        {:slots [0 0 0]}]}]
    ["Diamond" {:type :heraldry.charge-group.type/rows
                :strip-angle 45
                :spacing 25
                :stretch 1
                :charges [{:type :heraldry.charge.type/roundel
                           :field {:type :heraldry.field.type/plain
                                   :tincture :azure}}]
                :strips [{:slots [0 0 0]}
                         {:slots [0 0 0]}
                         {:slots [0 0 0]}]}]
    ["Semy rows" {:type :heraldry.charge-group.type/rows
                  :strip-angle 0
                  :spacing 20
                  :stretch 0.866
                  :charges [{:type :heraldry.charge.type/roundel
                             :field {:type :heraldry.field.type/plain
                                     :tincture :azure}}]
                  :strips [{:slots [0 0 0 0]}
                           {:slots [0 0 0]}
                           {:slots [0 0 0 0]}
                           {:slots [0 0 0]}
                           {:slots [0 0 0 0]}]}]
    ["Semy columns" {:type :heraldry.charge-group.type/columns
                     :strip-angle 0
                     :spacing 17
                     :stretch 0.866
                     :charges [{:type :heraldry.charge.type/roundel
                                :field {:type :heraldry.field.type/plain
                                        :tincture :azure}}]
                     :strips [{:slots [0 0 0 0]}
                              {:slots [0 0 0]}
                              {:slots [0 0 0 0]}
                              {:slots [0 0 0]}
                              {:slots [0 0 0 0]}]}]
    ["Frame" {:type :heraldry.charge-group.type/rows
              :strip-angle 0
              :spacing 17
              :stretch 1
              :charges [{:type :heraldry.charge.type/roundel
                         :field {:type :heraldry.field.type/plain
                                 :tincture :azure}}]
              :strips [{:slots [0 0 0 0 0]}
                       {:slots [0 nil nil nil 0]}
                       {:slots [0 nil nil nil 0]}
                       {:slots [0 nil nil nil 0]}
                       {:slots [0 0 0 0 0]}]}]]
   ["Arc"
    ["In annullo" {:type :heraldry.charge-group.type/arc
                   :charges [{:type :heraldry.charge.type/roundel
                              :field {:type :heraldry.field.type/plain
                                      :tincture :azure}}]
                   :slots [0 0 0 0 0 0 0]}]
    ["Arc" {:type :heraldry.charge-group.type/arc
            :start-angle 10
            :arc-angle 180
            :charges [{:type :heraldry.charge.type/roundel
                       :field {:type :heraldry.field.type/plain
                               :tincture :azure}}]
            :slots [0 0 0 0 0]}]
    ["In annullo (point to center)" {:type :heraldry.charge-group.type/arc
                                     :rotate-charges? true
                                     :charges [{:type :heraldry.charge.type/billet
                                                :field {:type :heraldry.field.type/plain
                                                        :tincture :azure}
                                                :anchor {:point :angle
                                                         :angle -180}}]
                                     :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] -180}]
    ["In annullo (follow)" {:type :heraldry.charge-group.type/arc
                            :rotate-charges? true
                            :charges [{:type :heraldry.charge.type/billet
                                       :field {:type :heraldry.field.type/plain
                                               :tincture :azure}
                                       :anchor {:point :angle
                                                :angle -90}}]
                            :slots [0 0 0 0 0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] -90}]
    ["Sheaf of" {:type :heraldry.charge-group.type/arc
                 :start-angle -45
                 :arc-angle 90
                 :radius 0
                 :rotate-charges? true
                 :charges [{:type :heraldry.charge.type/billet
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}
                            :geometry {:size 50
                                       :stretch 3}}]
                 :slots [0 0 0]}
     {[:charges 0 :anchor :point] :angle
      [:charges 0 :anchor :angle] 0
      [:charges 0 :geometry :size] 50}]]])

(rf/reg-event-db :select-charge-group-preset
  ;; TODO: this must not be an fn-traced, can be done once
  ;; https://github.com/day8/re-frame-debux/issues/40 is resolved
  (fn [db [_ path charge-group-preset charge-adjustments]]
    (let [new-db (-> db
                     (update-in path (fn [charge-group]
                                       (-> charge-group-preset
                                           (assoc :charges (:charges charge-group)))))
                     (assoc-in (conj path :charges 0 :anchor :point) :angle)
                     (assoc-in (conj path :charges 0 :anchor :angle) 0)
                     (assoc-in (conj path :charges 0 :geometry :size) nil))]
      (loop [new-db new-db
             [[rel-path value] & rest] charge-adjustments]
        (if (not rel-path)
          new-db
          (recur
           (assoc-in new-db (concat path rel-path) value)
           rest))))))

(defn charge-group-preset-choice [path group charge-adjustments display-name]
  (let [{:keys [result]} (render/coat-of-arms
                          [:context :coat-of-arms]
                          100
                          (-> shared/coa-select-option-context
                              (assoc :coat-of-arms
                                     {:escutcheon :rectangle
                                      :field {:type :heraldry.field.type/plain
                                              :tincture :argent
                                              :components [group]}})))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:select-charge-group-preset path group charge-adjustments])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:transform "translate(10,10)"}
       result]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn charge-group-preset-select [path]
  [:div.ui-setting
   [:label "Presets"]
   [:div.option
    [submenu/submenu path "Select Charge Group Preset" "Select" {:width "22em"}
     (for [[group-name & group] presets]
       ^{:key group-name}
       [:<>
        [:h4 group-name]
        (for [[display-name charge-group charge-adjustments] group]
          ^{:key display-name}
          [charge-group-preset-choice path charge-group charge-adjustments display-name])])]]])
