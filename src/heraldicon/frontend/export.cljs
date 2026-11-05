(ns heraldicon.frontend.export
  "Exports rendered in the browser from the data the page has already loaded,
  with the same code as the server's exports."
  (:require
   ["paper" :as paper]
   [com.wsscode.async.async-cljs :refer [<!p <? go-catch]]
   [heraldicon.entity.arms :as entity.arms]
   [heraldicon.export.arms :as export.arms]
   [heraldicon.export.collection :as export.collection]
   [heraldicon.export.core :as export]
   [heraldicon.font :as font]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [heraldicon.heraldry.default :as default]
   [heraldicon.render.texture :as texture]
   [heraldicon.util.async :as util.async]
   [heraldicon.util.sanitize :as sanitize]
   [re-frame.core :as rf]))

(defn- arms-render-data
  "The arms with their charges, or nil while a charge is still loading. Call
  it in a reactive context."
  [entity-data]
  (let [charges (into {}
                      (map (fn [{:keys [id version] :as variant}]
                             [variant @(rf/subscribe [::entity-for-rendering/data id version])]))
                      (entity.arms/charge-variants (-> entity-data :data :achievement)))]
    (when (every? (comp #{:done :error} :status) (vals charges))
      {:entity-data entity-data
       :charge-data-map (update-vals charges (fn [{:keys [status entity]}]
                                               ;; what the server renders for a
                                               ;; charge it can't fetch
                                               (if (= status :error)
                                                 {:data {:edn-data nil}}
                                                 entity)))})))

(defn- collection-render-data [entity-data]
  (let [elements (mapv (fn [{{:keys [id version]} :reference}]
                         (if id
                           (let [{:keys [status entity]} @(rf/subscribe [::entity-for-rendering/data id version])]
                             (case status
                               :done (arms-render-data entity)
                               ;; the server renders arms it can't fetch without data
                               :error (arms-render-data nil)
                               nil))
                           (arms-render-data default/arms-entity)))
                       (-> entity-data :data :elements))]
    (when (every? some? elements)
      {:entity-data entity-data
       :elements elements})))

(defn render-data
  "What an export of the entity renders, or nil while some of it is still
  loading. Call it in a reactive context, such as an action of the menu."
  [entity-type entity-data]
  (case entity-type
    :heraldicon.entity.type/arms (arms-render-data entity-data)
    :heraldicon.entity.type/collection (collection-render-data entity-data)))

(defn- fetch-base64 [url]
  (-> (js/fetch url)
      (.then (fn [^js response]
               (if (.-ok response)
                 (.blob response)
                 (throw (ex-info "Fetching an export asset failed" {:url url
                                                                    :status (.-status response)})))))
      (.then (fn [blob]
               (js/Promise.
                (fn [resolve reject]
                  (let [reader (js/FileReader.)]
                    (set! (.-onload reader) #(let [data-url (.-result reader)]
                                               (resolve (subs data-url (inc (.indexOf data-url ","))))))
                    (set! (.-onerror reader) #(reject (.-error reader)))
                    (.readAsDataURL reader blob))))))))

(defn- fetch-font-data-uris [fonts]
  (go-catch
   (let [fonts (vec (distinct (filter font/path-map fonts)))
         data (<!p (js/Promise.all
                    (clj->js (map #(fetch-base64 (js/encodeURI (str "/font" (font/path-map %))))
                                  fonts))))]
     (zipmap fonts (map export/font-data-uri fonts data)))))

(defn- with-texture-link
  "Adds the arms' texture as a data URI, as the server does, because an SVG
  drawn as an image loads nothing from outside."
  [{:keys [entity-data] :as arms-render-data}]
  (go-catch
   (let [texture-path (some-> entity-data :data :achievement :render-options :texture texture/full-path)]
     (cond-> arms-render-data
       (seq texture-path) (assoc :texture-link (str "data:image/jpeg;base64,"
                                                    (<!p (fetch-base64 texture-path))))))))

(defn- with-render-state [f]
  ;; the app draws into paper's global scope, so the render must not remove it
  (export/with-render-state paper f))

(defn- arms-svg [render-data opts]
  (go-catch
   (let [render-data (<? (with-texture-link render-data))
         fonts (with-render-state #(export.arms/used-fonts render-data opts))
         font-data-uris (<? (fetch-font-data-uris fonts))]
     (with-render-state #(export.arms/render-svg render-data (assoc opts :font-data-uris font-data-uris))))))

(defn- collection-svg [{:keys [entity-data elements]} opts]
  (go-catch
   (let [elements (<? (util.async/wait-for-all (mapv with-texture-link elements)))
         fonts (with-render-state #(export.collection/used-fonts entity-data opts))
         font-data-uris (<? (fetch-font-data-uris fonts))]
     (with-render-state #(export.collection/render-svg entity-data elements
                                                       (assoc opts :font-data-uris font-data-uris))))))

(defn next-paint
  "A promise that resolves once the browser has painted, e.g. a loading
  indicator, before work that blocks the page."
  []
  (js/Promise. (fn [resolve]
                 (js/requestAnimationFrame #(js/setTimeout resolve 0)))))

(defn render-svg
  "A channel with the SVG markup of the entity, for its render-data."
  [entity-type render-data & {:as opts}]
  (go-catch
   (<!p (next-paint))
   (<? (case entity-type
         :heraldicon.entity.type/arms (arms-svg render-data opts)
         :heraldicon.entity.type/collection (collection-svg render-data opts)))))

(defn filename [entity-type entity-data suffix]
  (str (sanitize/filename (:name entity-data)
                          (case entity-type
                            :heraldicon.entity.type/arms "arms"
                            :heraldicon.entity.type/collection "collection"))
       suffix))

(defn svg-dimensions
  "The width and height of the root svg element in the markup."
  [svg-data]
  (when-let [tag (re-find #"<svg[^>]*>" svg-data)]
    (let [w (some-> (re-find #" width=\"([^\"]+)\"" tag) second js/parseFloat)
          h (some-> (re-find #" height=\"([^\"]+)\"" tag) second js/parseFloat)]
      (when (and w h)
        [w h]))))

(defn- resize-svg [svg-data width height]
  (let [tag (re-find #"<svg[^>]*>" svg-data)
        resized (-> tag
                    (.replace #" width=\"[^\"]+\"" (str " width=\"" width "\""))
                    (.replace #" height=\"[^\"]+\"" (str " height=\"" height "\"")))]
    (.replace svg-data tag (constantly resized))))

(defn canvas-fits?
  "Whether the browser can draw a canvas of this size; browsers limit its
  side and area, Safari on iOS to about 16.7 megapixels."
  [width height]
  (let [canvas (js/document.createElement "canvas")]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    (let [fits? (when-let [ctx (.getContext canvas "2d")]
                  (.fillRect ctx (dec width) (dec height) 1 1)
                  (= 255 (aget (.-data (.getImageData ctx (dec width) (dec height) 1 1)) 3)))]
      ;; release the memory now rather than at the next garbage collection
      (set! (.-width canvas) 0)
      (set! (.-height canvas) 0)
      (boolean fits?))))

(defn svg->png
  "A promise of a PNG blob of the SVG markup at width by height pixels. The
  SVG must be self-contained, because an image loads nothing from outside."
  [svg-data width height]
  (let [url (js/URL.createObjectURL (js/Blob. #js [(resize-svg svg-data width height)]
                                              #js {:type "image/svg+xml"}))
        image (js/Image. width height)]
    (set! (.-src image) url)
    (-> (.decode image)
        (.then (fn []
                 (let [canvas (js/document.createElement "canvas")]
                   (set! (.-width canvas) width)
                   (set! (.-height canvas) height)
                   (.drawImage (.getContext canvas "2d") image 0 0 width height)
                   (js/Promise.
                    (fn [resolve reject]
                      (.toBlob canvas
                               (fn [blob]
                                 (set! (.-width canvas) 0)
                                 (set! (.-height canvas) 0)
                                 (if blob
                                   (resolve blob)
                                   (reject (ex-info "PNG generation failed" {:width width
                                                                             :height height}))))
                               "image/png"))))))
        (.finally #(js/URL.revokeObjectURL url)))))
