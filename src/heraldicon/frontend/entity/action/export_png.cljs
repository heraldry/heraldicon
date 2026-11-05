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
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private long-sides
  [1000 2000 4000 8000])

(defn- sizes
  "The pixel sizes of the presets, with whether the browser can draw them."
  [[width height]]
  (let [sizes (map (fn [long-side]
                     (let [scale (/ long-side (max width height))]
                       [(js/Math.round (* width scale))
                        (js/Math.round (* height scale))]))
                   long-sides)
        ;; a size fits if a larger one does, so testing stops at the first fit
        fits (loop [[[w h :as size] & smaller] (reverse sizes)
                    fits #{}]
               (cond
                 (nil? size) fits
                 (export/canvas-fits? w h) (into (conj fits size) smaller)
                 :else (recur smaller fits)))]
    (map (fn [size]
           [size (contains? fits size)])
         sizes)))

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

(defn- size-choice [svg-data filename sizes]
  [:div.buttons {:style {:display "flex"
                         :flex-wrap "wrap"
                         :gap "10px"}}
   (for [[[width height] fits?] sizes]
     ^{:key width}
     [:button.button {:type "button"
                      :disabled (not fits?)
                      :on-click #(rf/dispatch [::export svg-data width height filename])}
      (str width " × " height " px")])])

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
                        [size-choice svg-data filename (sizes (export/svg-dimensions svg-data))]))

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
