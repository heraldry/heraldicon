(ns heraldicon.frontend.entity.action.export-png
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<!p <?]]
   [heraldicon.frontend.entity.action.export-download :as export-download]
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.export :as export]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.localization.string :as string]
   [hodgepodge.core :refer [get-item local-storage set-item]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [taoensso.timbre :as log]))

(def ^:private local-storage-width-name
  "cl-png-export-width")

(def ^:private default-width
  2000)

(def ^:private min-width
  250)

(defn- pixel-size [[width height] target-width]
  [target-width (js/Math.round (* height (/ target-width width)))])

(defn- max-width
  "The largest width the browser's canvas can hold, of a few candidates."
  [dimensions]
  (or (first (filter #(apply export/canvas-fits? (pixel-size dimensions %))
                     [8000 6000 4000 3000 2000 1000]))
      min-width))

(defn- clamp-width [value max-width]
  (-> (if (js/isNaN value)
        default-width
        (js/Math.round value))
      (max min-width)
      (min max-width)))

(rf/reg-fx ::export-png
  (fn [[svg-data width height filename]]
    (modal/start-loading)
    (go
      (try
        (<!p (export/next-paint))
        (export-download/trigger-blob (<!p (export/svg->png svg-data width height)) filename)
        (modal/clear)

        (catch :default e
          (log/error e "generate png error"))

        (finally
          (modal/stop-loading))))))

(defn- size-choice [_svg-data _filename max-width]
  ;; the field's text, so typing isn't clamped before it's done
  (let [text (r/atom (str (clamp-width (js/parseFloat (get-item local-storage local-storage-width-name))
                                       max-width)))]
    (fn [svg-data filename max-width]
      (let [dimensions (export/svg-dimensions svg-data)
            [width height] (pixel-size dimensions (clamp-width (js/parseFloat @text) max-width))]
        [:div {:style {:min-width "24em"
                       :max-width "30em"}}
         [:p [tr :string.miscellaneous/png-width-hint]]
         [:div {:style {:display "flex"
                        :align-items "center"
                        :gap "10px"}}
          [:input {:type "range"
                   :min min-width
                   :max max-width
                   :step 50
                   :value width
                   :on-change #(reset! text (-> % .-target .-value))
                   :style {:flex "auto"}}]
          [:input {:type "number"
                   :min min-width
                   :max max-width
                   :value @text
                   :on-change #(reset! text (-> % .-target .-value))
                   :on-blur #(reset! text (str width))
                   :style {:width "5em"}}]]
         [:div.buttons {:style {:display "flex"
                                :align-items "center"
                                :margin-top "10px"}}
          [:span {:style {:flex "auto"}}
           (str width " × " height " px")]
          [:button.button
           {:type "button"
            :style {:margin-left "10px"}
            :on-click #(modal/clear)}
           [tr :string.button/cancel]]
          [:button.button.primary
           {:type "button"
            :style {:margin-left "10px"}
            :on-click #(do
                         (set-item local-storage local-storage-width-name (str width))
                         (rf/dispatch [::export svg-data width height filename]))}
           [tr :string.button/export]]]]))))

(rf/reg-event-fx ::export
  (fn [_ [_ svg-data width height filename]]
    {::export-png [svg-data width height filename]}))

(rf/reg-fx ::choose-size
  (fn [[entity-type render-data]]
    (modal/start-loading)
    (go
      (try
        ;; masks, as in the SVG export, antialias edges better than clips
        (let [svg-data (<? (export/render-svg entity-type render-data
                                              :add-short-url? true
                                              :add-url? true))
              filename (export/filename entity-type (:entity-data render-data) ".png")]
          (modal/create [:<> [tr :string.button/export] " (PNG)"]
                        [size-choice svg-data filename (max-width (export/svg-dimensions svg-data))]))

        (catch :default e
          (log/error e "generate png error"))

        (finally
          (modal/stop-loading))))))

(rf/reg-event-fx ::invoke
  (fn [_ [_ entity-type render-data]]
    {::choose-size [entity-type render-data]}))

(defn action [entity-type]
  (when (#{:heraldicon.entity.type/arms
           :heraldicon.entity.type/collection} entity-type)
    (let [form-db-path (form/data-path entity-type)
          logged-in? @(rf/subscribe [::session/logged-in?])
          can-export? (and logged-in?
                           @(rf/subscribe [::entity/saved? form-db-path])
                           (not @(rf/subscribe [::form/unsaved-changes? entity-type])))
          render-data (when can-export?
                        (export/render-data entity-type @(rf/subscribe [:get form-db-path])))]
      {:title (string/str-tr :string.button/export " (PNG)")
       :icon "fas fa-file-export"
       :handler (when render-data
                  #(rf/dispatch [::invoke entity-type render-data]))
       :disabled? (not render-data)
       :tooltip (when-not can-export?
                  (if (not logged-in?)
                    :string.user.message/need-to-be-logged-in
                    :string.user.message/save-changes-first))})))
