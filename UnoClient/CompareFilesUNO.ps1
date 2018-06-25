param([string]$param, [bool]$showError = 1)

# .\compare_powershell.ps1 0000
# .\compare_powershell.ps1 0000 0
# output: line error, IN, OUT. SOL

# $fileOUT = Get-Content $pathOUT
# $fileSOL = Get-Content $pathSOL
# $fileIN = Get-Content $pathIN

$fileOUT = Get-Content (".\Uno-" + $param + ".OUT")
$fileSOL = Get-Content (".\Uno-" + $param + ".SOL")
$fileIN = Get-Content (".\Uno-" + $param + ".IN")

"check for test Uno-" + $param
$c = 0
for ($i = 0; $i -le ($fileOUT.Count - 1); $i++) {
    if ($fileOUT[$i] -ne $fileSOL[$i]) {
        $c++
        if ($showError)
        {
            $output = "Line error {0} - operation:{1}     out:{2} sol:{3}" -f ($i + 1), ($fileIN[$i+1]), ($fileOUT[$i]), ($fileSOL[$i])
            $output
        }
    }
}

"check for test Uno-" + $param + " end. " + $c + " errors"
