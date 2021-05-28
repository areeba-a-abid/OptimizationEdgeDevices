# Optimization of Diagnostic Radiology Imaging Models for Edge Devices

- This repository provides all the source code written for the paper **`ML for Health Equity: Optimizing Diagnostic Models for Edge Devices`**.

## Directory Information

**`AndroidBenchmarkingApp`**: Contains Android Application for benchmarking model performance on Android devices.

**`model_optimization`**: Contains source code and weights for post training optimizations and qunatization aware training. It also contains code for performance evaluation (AUC-ROC score) of models.

**`inference_timing`**: Contains source code for calculating inference time on devices (other than Android, for that use `AndroidBenchmarkingApp`)

## NOTES:
- Change `NIH_14_DATASET_PATH` in all the notebooks inside `model_optimization` directory accordingly. This code assumes that NIH 14 datset is extracted as `NIH_14` inside the `model_optimization` directory.

- `test_images` inside `inference_timimg` directory is radomly sampled from main dataset. Their sole purpose is to calculate timings.

- `weights` directory inside `model_optimization` contains all the weights used for this paper. One can directly use it or make their own with the help of `quantize_models.ipynb` notebook. The name of weight files are self explanatory.

- Weight file inside `./AndroidBenchmarkingApp/app/src/main/assets` are the same as in `./model_optimization/weights` and maps as:
  - `QAT_int_8_v1.tflite` : **`INT8.tflite`**
  - `chexnet_kaggle_tflite_dynamic_quantization.tflite` : **`dynamic_quantization.tflite`**
  - `chexnet_kaggle_tflite_fp16.tflite` : **`FP16.tflite`** 

## Contributing Guidelines

- Contributors are encouraged to open github issue and send their merge requests.

## Contributors
- `Areeba Abid`
- `Priyanshu Sinha`
